package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.checkin.CheckInSchedule
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import com.domina.cycle.domain.pregnancy.AppMode
import com.domina.cycle.domain.reminders.PeriodLogPrompt
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** Fires at each daily slot: posts a period-logging notification if appropriate, then re-arms. */
class PeriodReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getIntExtra(PeriodReminderScheduler.EXTRA_SLOT, 0)
        val ep = EntryPointAccessors
            .fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
        val settings = ep.settings()
        val dayLogs = ep.dayLogRepository()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val reminderSettings = settings.reminderSettings.first()
                val mode = settings.appMode.first()
                if (reminderSettings.periodLogReminders &&
                    mode != AppMode.PREGNANCY &&
                    !CheckInSchedule.isQuietHour(LocalTime.now().hour)
                ) {
                    val today = LocalDate.now()
                    val logs = dayLogs.observeRange(today.minusDays(60), today).first()
                    val periods = PeriodDeriver.derive(
                        logs.filter { it.flow != FlowIntensity.NONE }.map { it.date }
                    )
                    val pred = CyclePredictor.predict(periods, today)
                    if (PeriodLogPrompt.shouldPromptOn(today, logs, pred, periods)) {
                        Notifier(context).notifyPeriodLog(slot)
                    }
                }
                PeriodReminderScheduler(context).rescheduleSlot(slot)  // arm tomorrow
            } finally {
                pending.finish()
            }
        }
    }
}
