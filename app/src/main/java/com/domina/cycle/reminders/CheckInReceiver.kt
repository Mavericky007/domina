package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.domina.cycle.domain.checkin.CheckInSchedule
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** Fires at each daily slot: posts the right check-in notification for the current mode, then re-arms. */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getIntExtra(CheckInScheduler.EXTRA_SLOT, 0)
        val settings = EntryPointAccessors
            .fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
            .settings()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val quiet = CheckInSchedule.isQuietHour(LocalTime.now().hour)
                if (settings.reminderSettings.first().checkIns && !quiet) {
                    val mode = settings.appMode.first()
                    val kind = CheckInSchedule.kindForSlot(mode, LocalDate.now(), slot)
                    Notifier(context).notifyCheckIn(slot, kind)
                }
                CheckInScheduler(context).rescheduleSlot(slot)  // arm tomorrow
            } finally {
                pending.finish()
            }
        }
    }
}
