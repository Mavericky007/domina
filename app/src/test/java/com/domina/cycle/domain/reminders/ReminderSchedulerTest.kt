package com.domina.cycle.domain.reminders

import com.domina.cycle.domain.prediction.Confidence
import com.domina.cycle.domain.prediction.CyclePhase
import com.domina.cycle.domain.prediction.CyclePrediction
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderSchedulerTest {
    private fun pred(
        next: LocalDate? = LocalDate.parse("2026-06-01"),
        fertileStart: LocalDate? = LocalDate.parse("2026-05-13"),
        ovulation: LocalDate? = LocalDate.parse("2026-05-18"),
    ) = CyclePrediction(
        cycleDay = 9, phase = CyclePhase.FOLLICULAR,
        averageCycleLength = 28, averagePeriodLength = 5,
        nextPeriodDate = next, ovulationDate = ovulation,
        fertileWindowStart = fertileStart, fertileWindowEnd = ovulation?.plusDays(1),
        confidence = Confidence.HIGH,
    )
    private val allOn = ReminderSettings(
        periodAlerts = true, fertilityAlerts = true, dailyNudge = true,
        dailyNudgeTime = LocalTime.of(20, 0), periodLeadDays = 2, morningHour = 9,
    )
    private val now = LocalDateTime.parse("2026-05-10T07:00:00")

    @Test fun allDisabledProducesNothing() {
        val s = allOn.copy(periodAlerts = false, fertilityAlerts = false, dailyNudge = false)
        assertThat(ReminderScheduler.compute(pred(), s, now)).isEmpty()
    }

    @Test fun periodAlertsProduceSoonTodayLate() {
        val r = ReminderScheduler.compute(pred(), allOn.copy(fertilityAlerts = false, dailyNudge = false), now)
        val types = r.map { it.type }
        assertThat(types).containsExactly(
            ReminderType.PERIOD_SOON, ReminderType.PERIOD_TODAY, ReminderType.PERIOD_LATE)
        val soon = r.first { it.type == ReminderType.PERIOD_SOON }
        assertThat(soon.fireAt).isEqualTo(LocalDateTime.parse("2026-05-30T09:00:00")) // Jun1 - 2 @ 9
        assertThat(r.first { it.type == ReminderType.PERIOD_TODAY }.fireAt)
            .isEqualTo(LocalDateTime.parse("2026-06-01T09:00:00"))
        assertThat(r.first { it.type == ReminderType.PERIOD_LATE }.fireAt)
            .isEqualTo(LocalDateTime.parse("2026-06-03T09:00:00")) // +2 days
    }

    @Test fun fertilityAlertsProduceWindowAndOvulation() {
        val n = LocalDateTime.parse("2026-05-01T07:00:00")
        val r = ReminderScheduler.compute(pred(), allOn.copy(periodAlerts = false, dailyNudge = false), n)
        assertThat(r.map { it.type })
            .containsExactly(ReminderType.FERTILE_WINDOW_OPEN, ReminderType.OVULATION_DAY)
        assertThat(r.first { it.type == ReminderType.OVULATION_DAY }.fireAt)
            .isEqualTo(LocalDateTime.parse("2026-05-18T09:00:00"))
    }

    @Test fun dailyNudgeFiresTodayIfTimeNotYetPassed() {
        val r = ReminderScheduler.compute(pred(), allOn.copy(periodAlerts = false, fertilityAlerts = false), now)
        assertThat(r.single().type).isEqualTo(ReminderType.DAILY_LOG_NUDGE)
        assertThat(r.single().fireAt).isEqualTo(LocalDateTime.parse("2026-05-10T20:00:00"))
    }

    @Test fun dailyNudgeRollsToTomorrowIfTimePassed() {
        val late = LocalDateTime.parse("2026-05-10T21:00:00")
        val r = ReminderScheduler.compute(pred(), allOn.copy(periodAlerts = false, fertilityAlerts = false), late)
        assertThat(r.single().fireAt).isEqualTo(LocalDateTime.parse("2026-05-11T20:00:00"))
    }

    @Test fun pastRemindersAreExcluded() {
        // now is after Jun 1 -> PERIOD_SOON/TODAY are in the past, only LATE (Jun 3) remains
        val n = LocalDateTime.parse("2026-06-02T07:00:00")
        val r = ReminderScheduler.compute(pred(), allOn.copy(fertilityAlerts = false, dailyNudge = false), n)
        assertThat(r.map { it.type }).containsExactly(ReminderType.PERIOD_LATE)
    }
}
