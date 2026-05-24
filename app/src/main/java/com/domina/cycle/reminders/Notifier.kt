package com.domina.cycle.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.domina.cycle.R
import com.domina.cycle.domain.reminders.ReminderType

class Notifier(private val context: Context) {

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
}
