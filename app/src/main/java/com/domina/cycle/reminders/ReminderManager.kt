package com.domina.cycle.reminders

import android.content.Context
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.repository.AppointmentRepository
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.data.repository.MedicationRepository
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import com.domina.cycle.domain.reminders.MedAppointmentScheduler
import com.domina.cycle.domain.reminders.ReminderScheduler
import com.domina.cycle.domain.reminders.ReminderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/** Reads logs + settings, recomputes the prediction, and (re)schedules all alarms. */
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DayLogRepository,
    private val settingsProvider: ReminderSettingsProvider,
    private val medicationRepository: MedicationRepository,
    private val appointmentRepository: AppointmentRepository,
) {
    suspend fun reschedule() {
        val today = LocalDate.now()
        val logs = repository.observeRange(today.minusDays(400), today).first()
        val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
        val prediction = CyclePredictor.predict(periods, today)
        val settings: ReminderSettings = settingsProvider.current()
        val reminders = ReminderScheduler.compute(prediction, settings, LocalDateTime.now())
        AlarmScheduler(context).reschedule(reminders)

        val meds = medicationRepository.observeAll().first().map {
            MedAppointmentScheduler.MedInput(it.id, it.name, it.timeMinutes, it.enabled)
        }
        val appts = appointmentRepository.observeAll().first().map {
            MedAppointmentScheduler.ApptInput(it.id, it.title, it.atEpochMillis, it.leadHours, it.enabled)
        }
        val items = MedAppointmentScheduler.compute(meds, appts, LocalDateTime.now(), ZoneId.systemDefault())
        ItemAlarmScheduler(context).scheduleAll(items)
    }
}
