package com.domina.cycle.domain.reminders

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class MedAppointmentSchedulerTest {
    private fun millis(dt: String) = LocalDateTime.parse(dt).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val now = LocalDateTime.parse("2026-05-10T07:00:00")

    @Test fun enabledMedSchedulesNextOccurrenceOfTime() {
        val items = MedAppointmentScheduler.compute(
            meds = listOf(MedAppointmentScheduler.MedInput(1, "Vitamin", 9 * 60, true)),
            appts = emptyList(), now = now, zone = ZoneOffset.UTC,
        )
        val it = items.single()
        assertThat(it.key).isEqualTo("med_1")
        assertThat(it.fireAt).isEqualTo(LocalDateTime.parse("2026-05-10T09:00:00")) // today, 9am ahead of 7am
        assertThat(it.title).contains("Vitamin")
    }

    @Test fun medTimeAlreadyPassedRollsToTomorrow() {
        val items = MedAppointmentScheduler.compute(
            meds = listOf(MedAppointmentScheduler.MedInput(1, "Pill", 6 * 60, true)),
            appts = emptyList(), now = now, zone = ZoneOffset.UTC,
        )
        assertThat(items.single().fireAt).isEqualTo(LocalDateTime.parse("2026-05-11T06:00:00"))
    }

    @Test fun disabledItemsAreSkipped() {
        val items = MedAppointmentScheduler.compute(
            meds = listOf(MedAppointmentScheduler.MedInput(1, "X", 9 * 60, false)),
            appts = listOf(MedAppointmentScheduler.ApptInput(2, "Y", millis("2026-06-01T10:00:00"), 24, false)),
            now = now, zone = ZoneOffset.UTC,
        )
        assertThat(items).isEmpty()
    }

    @Test fun appointmentFiresLeadHoursBefore() {
        val items = MedAppointmentScheduler.compute(
            meds = emptyList(),
            appts = listOf(MedAppointmentScheduler.ApptInput(7, "OB visit", millis("2026-06-01T10:00:00"), 24, true)),
            now = now, zone = ZoneOffset.UTC,
        )
        val it = items.single()
        assertThat(it.key).isEqualTo("appt_7")
        assertThat(it.fireAt).isEqualTo(LocalDateTime.parse("2026-05-31T10:00:00")) // 24h before
    }

    @Test fun pastAppointmentsAreSkipped() {
        val items = MedAppointmentScheduler.compute(
            meds = emptyList(),
            appts = listOf(MedAppointmentScheduler.ApptInput(7, "Old", millis("2026-05-01T10:00:00"), 24, true)),
            now = now, zone = ZoneOffset.UTC,
        )
        assertThat(items).isEmpty()
    }
}
