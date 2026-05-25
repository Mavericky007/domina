package com.domina.cycle.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.domina.cycle.R
import com.domina.cycle.domain.checkin.CheckInKind
import com.domina.cycle.domain.checkin.CheckInPrompts
import com.domina.cycle.domain.reminders.ReminderType

class Notifier(private val context: Context) {

    /** A check-in prompt with up to 3 quick-tap buttons that record without opening the app. */
    fun notifyCheckIn(slot: Int, kind: CheckInKind) {
        NotificationChannels.ensureCreated(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val notifId = CHECKIN_NOTIF_BASE + slot
        val builder = NotificationCompat.Builder(context, NotificationChannels.CHECKIN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(CheckInPrompts.title(kind))
            .setContentText("A quick check-in 💛")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        CheckInPrompts.options(kind).forEach { opt ->
            val tap = Intent(context, CheckInActionReceiver::class.java).apply {
                putExtra(CheckInActionReceiver.EXTRA_KIND, kind.name)
                putExtra(CheckInActionReceiver.EXTRA_SCORE, opt.score)
                putExtra(CheckInActionReceiver.EXTRA_NOTIF_ID, notifId)
            }
            val pi = PendingIntent.getBroadcast(
                context, CHECKIN_ACTION_BASE + slot * 10 + opt.score, tap,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, "${opt.emoji} ${opt.label}", pi)
        }
        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    /** Replaces the prompt with a warm, encouraging reply that auto-dismisses. */
    fun notifyConfirmation(notifId: Int, message: String) {
        NotificationChannels.ensureCreated(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val n = NotificationCompat.Builder(context, NotificationChannels.CHECKIN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Got it 💛")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setTimeoutAfter(8_000)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(notifId, n)
    }

    fun notify(type: ReminderType, title: String, body: String) {
        NotificationChannels.ensureCreated(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) return // user hasn't granted notifications; skip silently

        val n = NotificationCompat.Builder(context, NotificationChannels.channelFor(type))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(type.ordinal, n)
    }

    fun notifyRaw(channelId: String, notificationId: Int, title: String, body: String) {
        NotificationChannels.ensureCreated(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        NotificationManagerCompat.from(context).notify(notificationId, n)
    }

    fun notifyPeriodLog(slot: Int) {
        NotificationChannels.ensureCreated(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        val notifId = PERIOD_NOTIF_BASE + slot
        val builder = NotificationCompat.Builder(context, NotificationChannels.CYCLE_LOG)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("How's your flow today?")
            .setContentText("Tap to log today's period 💛")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        listOf("LIGHT" to "Light", "MEDIUM" to "Medium", "HEAVY" to "Heavy").forEachIndexed { i, (flow, label) ->
            val tap = Intent(context, PeriodLogActionReceiver::class.java).apply {
                putExtra(PeriodLogActionReceiver.EXTRA_FLOW, flow)
                putExtra(PeriodLogActionReceiver.EXTRA_NOTIF_ID, notifId)
            }
            val pi = PendingIntent.getBroadcast(
                context, PERIOD_ACTION_BASE + slot * 10 + i, tap,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, label, pi)
        }
        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    fun notifyPeriodConfirmation(notifId: Int, message: String) {
        NotificationChannels.ensureCreated(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        val n = NotificationCompat.Builder(context, NotificationChannels.CYCLE_LOG)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Logged 💛").setContentText(message)
            .setAutoCancel(true).setTimeoutAfter(8_000)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
        NotificationManagerCompat.from(context).notify(notifId, n)
    }

    companion object {
        private const val CHECKIN_NOTIF_BASE = 7_000      // one notification id per slot
        private const val CHECKIN_ACTION_BASE = 910_000   // unique PendingIntent code per (slot, score)
        private const val PERIOD_NOTIF_BASE = 7_100
        private const val PERIOD_ACTION_BASE = 920_000
    }
}
