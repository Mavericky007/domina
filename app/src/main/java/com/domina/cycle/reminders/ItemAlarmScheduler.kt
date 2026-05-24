package com.domina.cycle.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.domina.cycle.domain.reminders.ScheduledItem
import java.time.ZoneId

class ItemAlarmScheduler(private val context: Context) {
    private val am = context.getSystemService<AlarmManager>()!!

    fun scheduleAll(items: List<ScheduledItem>) = items.forEach { schedule(it) }

    fun cancel(key: String) {
        PendingIntent.getBroadcast(
            context, requestCode(key), Intent(context, ItemReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { am.cancel(it) }
    }

    private fun schedule(item: ScheduledItem) {
        val intent = Intent(context, ItemReminderReceiver::class.java).apply {
            putExtra(ItemReminderReceiver.EXTRA_CHANNEL, item.channelId)
            putExtra(ItemReminderReceiver.EXTRA_TITLE, item.title)
            putExtra(ItemReminderReceiver.EXTRA_BODY, item.body)
            putExtra(ItemReminderReceiver.EXTRA_NOTIF_ID, requestCode(item.key))
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode(item.key), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = item.fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    // Stable, collision-resistant codes for dynamic items (kept clear of ReminderType ordinals 0..9).
    private fun requestCode(key: String): Int = 100_000 + (key.hashCode() and 0x7FFFFFFF) % 1_000_000
}
