package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.domina.cycle.domain.checkin.CheckInKind
import com.domina.cycle.domain.checkin.CheckInMessages
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Handles a tap on one of the notification's quick buttons: records it (no app open) + replies warmly. */
class CheckInActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { CheckInKind.valueOf(it) }.getOrNull() } ?: return
        val score = intent.getIntExtra(EXTRA_SCORE, 2)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val repository = EntryPointAccessors
            .fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
            .checkInRepository()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val now = LocalDateTime.now()
                repository.add(now.toLocalDate().toEpochDay(), now.hour * 60 + now.minute, kind, score)
                Notifier(context).notifyConfirmation(notifId, CheckInMessages.confirmation(kind, score))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_KIND = "checkin_kind"
        const val EXTRA_SCORE = "checkin_score"
        const val EXTRA_NOTIF_ID = "checkin_notif_id"
    }
}
