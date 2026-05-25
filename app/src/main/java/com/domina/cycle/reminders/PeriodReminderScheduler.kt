package com.domina.cycle.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Schedules twice-daily period-logging notifications (one exact alarm per slot). */
class PeriodReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService<AlarmManager>()!!

    fun scheduleAll() = SLOT_TIMES.indices.forEach { rescheduleSlot(it) }

    fun cancelAll() = SLOT_TIMES.indices.forEach { slot ->
        PendingIntent.getBroadcast(
            context, REQ_BASE + slot,
            Intent(context, PeriodReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { alarmManager.cancel(it) }
    }

    /** (Re)schedules a slot for its next occurrence (today if still ahead, else tomorrow). */
    fun rescheduleSlot(slot: Int) {
        val time = SLOT_TIMES[slot]
        val now = LocalDateTime.now()
        var fire = LocalDateTime.of(LocalDate.now(), time)
        if (!fire.isAfter(now)) fire = fire.plusDays(1)
        val triggerAt = fire.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, PeriodReminderReceiver::class.java).putExtra(EXTRA_SLOT, slot)
        val pi = PendingIntent.getBroadcast(
            context, REQ_BASE + slot, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    companion object {
        const val EXTRA_SLOT = "period_slot"
        private const val REQ_BASE = 930_000
        val SLOT_TIMES = listOf(LocalTime.of(10, 0), LocalTime.of(19, 0))
    }
}
