package com.domina.cycle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.data.prefs.ThemePreference
import com.domina.cycle.domain.reminders.ReminderSettings
import com.domina.cycle.reminders.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val reminderManager: ReminderManager,
) : ViewModel() {
    val theme: StateFlow<ThemePreference> =
        settings.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreference.SOFT_SWEET)
    val reminders: StateFlow<ReminderSettings> =
        settings.reminderSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderSettings())

    fun setTheme(t: ThemePreference) { viewModelScope.launch { settings.setTheme(t) } }

    fun updateReminders(s: ReminderSettings) {
        viewModelScope.launch { settings.setReminderSettings(s); reminderManager.reschedule() }
    }
}
