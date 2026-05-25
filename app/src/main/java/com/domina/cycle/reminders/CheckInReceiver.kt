package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.domain.checkin.CheckInSchedule
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Fires at each daily slot: posts the right check-in notification for the current mode, then re-arms. */
@AndroidEntryPoint
class CheckInReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getIntExtra(CheckInScheduler.EXTRA_SLOT, 0)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val quiet = CheckInSchedule.isQuietHour(java.time.LocalTime.now().hour)
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
