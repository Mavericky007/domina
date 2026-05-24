package com.domina.cycle.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.backup.BackupManager
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.data.prefs.ThemePreference
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.domain.reminders.ReminderSettings
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

    fun setTheme(t: ThemePreference) { viewModelScope.launch { settings.setTheme(t) } }

    fun updateReminders(s: ReminderSettings) {
        viewModelScope.launch { settings.setReminderSettings(s); reminderManager.reschedule() }
    }

    fun setMode(m: com.domina.cycle.domain.pregnancy.AppMode) { viewModelScope.launch { settings.setAppMode(m) } }
    fun setDueDate(date: java.time.LocalDate?) { viewModelScope.launch { settings.setDueDate(date) } }

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
