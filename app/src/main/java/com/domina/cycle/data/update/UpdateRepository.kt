package com.domina.cycle.data.update

import com.domina.cycle.domain.update.VersionCompare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class ReleaseInfo(val version: String, val apkUrl: String?, val notes: String)

sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus
    data class Available(val release: ReleaseInfo) : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

interface UpdateRepository {
    /** Asks GitHub for the latest release and compares it to [currentVersion]. */
    suspend fun check(currentVersion: String): UpdateStatus
}

/**
 * On-demand update check against the project's public GitHub Releases. The app only reaches the
 * network when this is invoked (no background polling), and only ever talks to api.github.com —
 * no personal data is sent. Requires the repo to be public for unauthenticated access.
 */
class GithubUpdateRepository @Inject constructor() : UpdateRepository {

    override suspend fun check(currentVersion: String): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Domina-Android")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            try {
                when (conn.responseCode) {
                    200 -> parse(conn.inputStream.bufferedReader().use { it.readText() }, currentVersion)
                    404 -> UpdateStatus.Error("No releases published yet.")
                    else -> UpdateStatus.Error("Couldn't reach GitHub (${conn.responseCode}).")
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            UpdateStatus.Error("No connection. Check your internet and try again.")
        }
    }

    private fun parse(body: String, currentVersion: String): UpdateStatus {
        val json = JSONObject(body)
        val tag = json.getString("tag_name")
        val notes = json.optString("body", "").trim()
        var apkUrl: String? = null
        json.optJSONArray("assets")?.let { assets ->
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.getString("browser_download_url"); break
                }
            }
        }
        return if (VersionCompare.isNewer(currentVersion, tag))
            UpdateStatus.Available(ReleaseInfo(tag.removePrefix("v").removePrefix("V"), apkUrl, notes))
        else UpdateStatus.UpToDate
    }

    companion object {
        const val OWNER = "Mavericky007"
        const val REPO = "domina"
    }
}
