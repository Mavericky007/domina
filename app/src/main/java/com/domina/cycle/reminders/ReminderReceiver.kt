package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.domina.cycle.domain.reminders.ReminderType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE)?.let { ReminderType.valueOf(it) } ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return
        Notifier(context).notify(type, title, body)
    }

    companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }
}
