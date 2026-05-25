package com.domina.cycle.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Downloads the release APK via the system DownloadManager and hands the finished file to the
 *  system installer. The OS always shows its own install-confirmation screen, so this is
 *  "tap Install, then approve" — never a silent self-replace.
 *
 *  The download survives leaving Settings and even the app process dying: the DownloadManager
 *  job runs in the system, and we persist its id so we can re-attach on the next launch
 *  ([syncFromPending]). Install is triggered by an explicit foreground tap ([install]) so it is
 *  never blocked by Android's background-activity-launch limits. */
@Singleton
class ApkUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface State {
        data object Idle : State
        data class Downloading(val percent: Int, val waitingForNetwork: Boolean = false) : State
        data object ReadyToInstall : State
        data object Installing : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dm get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs by lazy { context.getSharedPreferences("apk_updater", Context.MODE_PRIVATE) }

    @Volatile private var polling = false

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Opens system settings so the user can allow installs from Domina. */
    fun requestInstallPermission() {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Version label of the update currently being handled, for the UI ("Updating to v1.7"). */
    fun pendingVersion(): String? = prefs.getString(KEY_VERSION, null)

    fun startUpdate(url: String, version: String? = null) {
        if (_state.value is State.Downloading || _state.value is State.ReadyToInstall) return
        _state.value = State.Downloading(0)
        runCatching { targetFile().delete() }
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Domina update")
            .setDescription("Downloading the latest version")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)   // continue on cellular, not just Wi-Fi
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(context, null, "$SUBDIR/$FILENAME")
        val id = runCatching { dm.enqueue(request) }.getOrElse {
            _state.value = State.Failed("Couldn't start the download."); return
        }
        prefs.edit().putLong(KEY_ID, id).putString(KEY_URL, url).putString(KEY_VERSION, version).apply()
        startPolling(id)
    }

    /** Re-attach to a download that was in flight or finished while we were backgrounded / killed. */
    fun syncFromPending() {
        if (_state.value !is State.Idle || polling) return
        val id = prefs.getLong(KEY_ID, -1L)
        if (id < 0L) return
        // If the pending update is already the installed version, it applied successfully — forget it.
        val pendingVer = prefs.getString(KEY_VERSION, null)
        if (pendingVer != null && pendingVer == installedVersion()) { clearPending(); return }
        startPolling(id)
    }

    /** Hand the finished APK to the system installer. Called from a foreground tap. */
    fun install() {
        val file = targetFile()
        if (!file.exists()) { _state.value = State.Failed("The downloaded file is missing — try again."); return }
        if (!canInstall()) { requestInstallPermission(); return } // keep ReadyToInstall so they can tap again
        _state.value = State.Installing
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { _state.value = State.Failed("Couldn't open the installer.") }
    }

    /** Re-download after a failure, reusing the last URL. */
    fun retry() {
        val url = prefs.getString(KEY_URL, null) ?: run { _state.value = State.Idle; return }
        _state.value = State.Idle
        startUpdate(url, prefs.getString(KEY_VERSION, null))
    }

    fun cancel() {
        val id = prefs.getLong(KEY_ID, -1L)
        if (id >= 0L) runCatching { dm.remove(id) }
        clearPending()
        _state.value = State.Idle
    }

    fun reset() { _state.value = State.Idle }

    private fun startPolling(id: Long) {
        if (polling) return
        polling = true
        scope.launch { try { poll(id) } finally { polling = false } }
    }

    private suspend fun poll(id: Long) {
        while (true) {
            val cursor = dm.query(DownloadManager.Query().setFilterById(id))
            if (cursor == null) { _state.value = State.Failed("Download was cancelled."); return }
            cursor.use { c ->
                if (!c.moveToFirst()) { _state.value = State.Failed("Download was cancelled."); return }
                when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> { _state.value = State.ReadyToInstall; return }
                    DownloadManager.STATUS_FAILED ->
                        { _state.value = State.Failed("Download failed — check your connection and try again."); return }
                    DownloadManager.STATUS_PAUSED ->
                        _state.value = State.Downloading(percentOf(c), waitingForNetwork = true)
                    else -> _state.value = State.Downloading(percentOf(c)) // PENDING / RUNNING
                }
            }
            delay(500)
        }
    }

    private fun percentOf(c: Cursor): Int {
        val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        return if (total > 0L) ((done * 100) / total).toInt() else 0
    }

    private fun installedVersion(): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()

    private fun clearPending() { prefs.edit().remove(KEY_ID).remove(KEY_URL).remove(KEY_VERSION).apply() }
    private fun targetFile() = File(context.getExternalFilesDir(null), "$SUBDIR/$FILENAME")

    companion object {
        private const val APK_MIME = "application/vnd.android.package-archive"
        private const val SUBDIR = "updates"
        private const val FILENAME = "domina-update.apk"
        private const val KEY_ID = "pending_id"
        private const val KEY_URL = "pending_url"
        private const val KEY_VERSION = "pending_version"
    }
}
