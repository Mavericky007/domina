package com.domina.cycle.data.prefs

import com.domina.cycle.domain.reminders.ReminderSettings
import com.domina.cycle.reminders.ReminderSettingsProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SettingsReminderProvider @Inject constructor(
    private val settings: SettingsRepository,
) : ReminderSettingsProvider {
    override suspend fun current(): ReminderSettings = settings.reminderSettings.first()
}
