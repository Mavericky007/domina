package com.domina.cycle.reminders

import com.domina.cycle.domain.reminders.ReminderSettings

interface ReminderSettingsProvider { suspend fun current(): ReminderSettings }
