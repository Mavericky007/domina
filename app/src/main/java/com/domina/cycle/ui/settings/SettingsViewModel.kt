package com.domina.cycle.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.backup.BackupManager
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.data.prefs.ThemePreference
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.data.update.ReleaseInfo
import com.domina.cycle.data.update.UpdateRepository
import com.domina.cycle.data.update.UpdateStatus
import com.domina.cycle.domain.reminders.ReminderSettings
import com.domina.cycle.update.ApkUpdater
import com.domina.cycle.reminders.ReminderManager
import com.domina.cycle.report.ReportContent
import com.domina.cycle.report.PdfReportRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val reminderManager: ReminderManager,
    private val backupManager: BackupManager,
    private val dayLogRepository: DayLogRepository,
    private val updateRepository: UpdateRepository,
    val apkUpdater: ApkUpdater,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val theme: StateFlow<ThemePreference> =
        settings.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreference.SOFT_SWEET)
    val reminders: StateFlow<ReminderSettings> =
        settings.reminderSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderSettings())

    val mode: StateFlow<com.domina.cycle.domain.pregnancy.AppMode> =
        settings.appMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.domina.cycle.domain.pregnancy.AppMode.CYCLE)
    val dueDate: StateFlow<java.time.LocalDate?> =
        settings.dueDate.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val discreetIcon: StateFlow<Boolean> =
        settings.discreetIcon.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setDiscreetIcon(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDiscreetIcon(enabled)
            com.domina.cycle.launcher.DiscreetIconManager(context).setDiscreet(enabled)
        }
    }

    val userName: StateFlow<String?> =
        settings.userName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val birthDate: StateFlow<LocalDate?> =
        settings.birthDate.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val heightCm: StateFlow<Int?> =
        settings.heightCm.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- app updates (on-demand) ---
    sealed interface UpdateUiState {
        data object Idle : UpdateUiState
        data object Checking : UpdateUiState
        data object UpToDate : UpdateUiState
        data class Available(val release: ReleaseInfo) : UpdateUiState
        data class Error(val message: String) : UpdateUiState
    }

    val appVersion: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "?"

    private val _update = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()
    val download: StateFlow<ApkUpdater.State> = apkUpdater.state

    fun checkForUpdates() {
        _update.value = UpdateUiState.Checking
        viewModelScope.launch {
            _update.value = when (val r = updateRepository.check(appVersion)) {
                is UpdateStatus.UpToDate -> UpdateUiState.UpToDate
                is UpdateStatus.Available -> UpdateUiState.Available(r.release)
                is UpdateStatus.Error -> UpdateUiState.Error(r.message)
            }
        }
    }

    fun downloadUpdate(url: String) = apkUpdater.startUpdate(url)
    fun dismissDownloadError() = apkUpdater.reset()

    fun setTheme(t: ThemePreference) { viewModelScope.launch { settings.setTheme(t) } }

    fun updateReminders(s: ReminderSettings) {
        viewModelScope.launch { settings.setReminderSettings(s); reminderManager.reschedule() }
    }

    fun setMode(m: com.domina.cycle.domain.pregnancy.AppMode) { viewModelScope.launch { settings.setAppMode(m) } }
    fun setDueDate(date: java.time.LocalDate?) { viewModelScope.launch { settings.setDueDate(date) } }
    /** Set the due date from an estimated pregnancy start (last period) — LMP + 280 days. */
    fun setPregnancyStart(date: java.time.LocalDate) {
        viewModelScope.launch {
            settings.setDueDate(com.domina.cycle.domain.pregnancy.PregnancyCalculator.dueDateFromLmp(date))
        }
    }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun clearMessage() { _message.value = null }

    fun backupTo(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { backupManager.export(it, passphrase.toCharArray()) }
            }.onSuccess { _message.value = "Backup saved" }
                .onFailure { _message.value = "Backup failed: ${it.message}" }
        }
    }

    fun restoreFrom(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)!!.use { backupManager.import(it, passphrase.toCharArray()) }
            }.onSuccess { _message.value = "Restored. Reopen the app to see everything." }
                .onFailure { _message.value = "Restore failed — check your passphrase." }
        }
    }

    fun exportReport(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val logs = dayLogRepository.observeRange(
                    LocalDate.now().minusDays(400),
                    LocalDate.now()
                ).first()
                val report = ReportContent.build(logs)
                context.contentResolver.openOutputStream(uri)!!.use { PdfReportRenderer.render(report, it) }
            }.onSuccess { _message.value = "Report saved 💛" }
                .onFailure { _message.value = "Export failed: ${it.message}" }
        }
    }
}
