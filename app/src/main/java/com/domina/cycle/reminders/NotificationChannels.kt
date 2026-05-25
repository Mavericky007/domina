package com.domina.cycle.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.domina.cycle.domain.reminders.ReminderType

object NotificationChannels {
    const val CYCLE = "cycle_alerts"
    const val FERTILITY = "fertility_alerts"
    const val NUDGE = "daily_nudge"
    const val MEDS = "meds_appointments"
    const val CHECKIN = "mood_checkins"
    const val CYCLE_LOG = "cycle_log"

    fun channelFor(type: ReminderType): String = when (type) {
        ReminderType.PERIOD_SOON, ReminderType.PERIOD_TODAY, ReminderType.PERIOD_LATE -> CYCLE
        ReminderType.FERTILE_WINDOW_OPEN, ReminderType.OVULATION_DAY -> FERTILITY
        ReminderType.DAILY_LOG_NUDGE -> NUDGE
    }

    fun ensureCreated(context: Context) {
        val mgr = context.getSystemService<NotificationManager>() ?: return
        listOf(
            Triple(CYCLE, "Cycle & period alerts", NotificationManager.IMPORTANCE_HIGH),
            Triple(FERTILITY, "Fertility alerts", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(NUDGE, "Daily logging nudge", NotificationManager.IMPORTANCE_LOW),
            Triple(MEDS, "Medications & appointments", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHECKIN, "Mood check-ins", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CYCLE_LOG, "Period logging reminders", NotificationManager.IMPORTANCE_DEFAULT),
        ).forEach { (id, name, importance) ->
            mgr.createNotificationChannel(NotificationChannel(id, name, importance))
        }
    }
}
