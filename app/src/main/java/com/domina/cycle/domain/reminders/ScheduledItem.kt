package com.domina.cycle.domain.reminders

import java.time.LocalDateTime

/** A generic, id-keyed scheduled notification (used for meds & appointments). */
data class ScheduledItem(
    val key: String,        // stable, e.g. "med_3" / "appt_7"
    val fireAt: LocalDateTime,
    val channelId: String,
    val title: String,
    val body: String,
)
