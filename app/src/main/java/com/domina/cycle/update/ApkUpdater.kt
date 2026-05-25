package com.domina.cycle.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
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

/** Downloads the release APK and hands it to the system installer. The OS always shows its own
 *  install-confirmation screen, so this is "one tap, then approve" — never a silent self-replace. */
@Singleton
class ApkUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface State {
        data object Idle : State
        data class Downloading(val percent: Int) : State
        data object Installing : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dm get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Opens system settings so the user can allow installs from Domina. */
    fun requestInstallPermission() {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun startUpdate(url: String) {
        if (_state.value is State.Downloading) return
        _state.value = State.Downloading(0)
        runCatching { targetFile().delete() }
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Domina update")
            .setDescription("Downloading the latest version")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, null, "$SUBDIR/$FILENAME")
        val id = runCatching { dm.enqueue(request) }.getOrElse {
            _state.value = State.Failed("Couldn't start the download."); return
        }
        scope.launch { poll(id) }
    }

    private suspend fun poll(id: Long) {
        while (true) {
            val query = DownloadManager.Query().setFilterById(id)
            dm.query(query).use { c ->
                if (c == null || !c.moveToFirst()) { _state.value = State.Failed("Download was cancelled."); return }
                when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> { _state.value = State.Installing; install(); return }
                    DownloadManager.STATUS_FAILED -> { _state.value = State.Failed("Download failed."); return }
                    else -> {
                        val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        _state.value = State.Downloading(if (total > 0) ((done * 100) / total).toInt() else 0)
                    }
                }
            }
            delay(400)
        }
    }

    private fun install() {
        val file = targetFile()
        if (!file.exists()) { _state.value = State.Failed("Downloaded file is missing."); return }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { _state.value = State.Failed("Couldn't open the installer.") }
    }

    fun reset() { _state.value = State.Idle }

    private fun targetFile() = File(context.getExternalFilesDir(null), "$SUBDIR/$FILENAME")

    companion object {
        private const val APK_MIME = "application/vnd.android.package-archive"
        private const val SUBDIR = "updates"
        private const val FILENAME = "domina-update.apk"
    }
}
