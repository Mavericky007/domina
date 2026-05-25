package com.domina.cycle.domain.reminders

import java.time.LocalTime

data class ReminderSettings(
    val periodAlerts: Boolean = true,
    val fertilityAlerts: Boolean = true,
    val dailyNudge: Boolean = false,
    val dailyNudgeTime: LocalTime = LocalTime.of(20, 0),
    val periodLeadDays: Int = 2,
    val morningHour: Int = 9,
    val checkIns: Boolean = false,
)
