package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ItemReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 1000)
        Notifier(context).notifyRaw(channel, notifId, title, body)
    }
    companion object {
        const val EXTRA_CHANNEL = "channel"; const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"; const val EXTRA_NOTIF_ID = "notif_id"
    }
}
