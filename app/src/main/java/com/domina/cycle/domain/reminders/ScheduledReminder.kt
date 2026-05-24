package com.domina.cycle.domain.reminders

import java.time.LocalDateTime

data class ScheduledReminder(
    val type: ReminderType,
    val fireAt: LocalDateTime,
    val title: String,
    val body: String,
)
