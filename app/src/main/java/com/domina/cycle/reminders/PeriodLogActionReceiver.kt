package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Handles a tap on one of the period-log notification's quick buttons: saves flow, shows confirmation. */
class PeriodLogActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val flow = intent.getStringExtra(EXTRA_FLOW)
            ?.let { runCatching { FlowIntensity.valueOf(it) }.getOrNull() } ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
            .dayLogRepository()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val today = LocalDate.now()
                val existing = repo.getByDate(today) ?: DayLog(today)
                repo.save(existing.copy(flow = flow))
                Notifier(context).notifyPeriodConfirmation(
                    notifId,
                    "Logged ${flow.name.lowercase()} flow for today 💛",
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_FLOW = "period_flow"
        const val EXTRA_NOTIF_ID = "period_notif_id"
    }
}
