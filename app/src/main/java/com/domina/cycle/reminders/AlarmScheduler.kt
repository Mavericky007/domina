package com.domina.cycle.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.domina.cycle.domain.reminders.ReminderType
import com.domina.cycle.domain.reminders.ScheduledReminder
import java.time.ZoneId

/** Schedules/cancels exact alarms for the given reminders. One alarm per ReminderType. */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService<AlarmManager>()!!

    fun reschedule(reminders: List<ScheduledReminder>) {
        ReminderType.entries.forEach { cancel(it) }
        reminders.forEach { schedule(it) }
    }

    private fun schedule(r: ScheduledReminder) {
        val triggerAt = r.fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(r))
    }

    private fun cancel(type: ReminderType) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context, type.ordinal,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return
        )
    }

    private fun pendingIntent(r: ScheduledReminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TYPE, r.type.name)
            putExtra(ReminderReceiver.EXTRA_TITLE, r.title)
            putExtra(ReminderReceiver.EXTRA_BODY, r.body)
        }
        return PendingIntent.getBroadcast(
            context, r.type.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
