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

/** Schedules the 3 daily mood/symptom check-in notifications (one exact alarm per slot). */
class CheckInScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService<AlarmManager>()!!

    fun scheduleAll() = SLOT_TIMES.indices.forEach { rescheduleSlot(it) }

    fun cancelAll() = SLOT_TIMES.indices.forEach { slot ->
        PendingIntent.getBroadcast(
            context, REQ_BASE + slot,
            Intent(context, CheckInReceiver::class.java),
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
        val intent = Intent(context, CheckInReceiver::class.java).putExtra(EXTRA_SLOT, slot)
        val pi = PendingIntent.getBroadcast(
            context, REQ_BASE + slot, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    companion object {
        const val EXTRA_SLOT = "checkin_slot"
        private const val REQ_BASE = 900_000
        // Late morning, mid-afternoon, evening.
        val SLOT_TIMES = listOf(LocalTime.of(10, 30), LocalTime.of(15, 0), LocalTime.of(20, 0))
    }
}
