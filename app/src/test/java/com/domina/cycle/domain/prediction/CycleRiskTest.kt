package com.domina.cycle.domain.prediction

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.Intimacy
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class CycleRiskTest {
    private val lastStart = LocalDate.parse("2026-05-01")
    private val periods = listOf(LoggedPeriod(lastStart, 5))

    private fun prediction(next: LocalDate) = CyclePrediction(
        cycleDay = 1, phase = null, averageCycleLength = 28, averagePeriodLength = 5,
        nextPeriodDate = next, ovulationDate = null, fertileWindowStart = null,
        fertileWindowEnd = null, confidence = Confidence.HIGH,
    )

    @Test fun morningAfterPillDelaysNextPeriod() {
        val logs = listOf(DayLog(lastStart.plusDays(10), emergencyContraception = true))
        val pred = prediction(lastStart.plusDays(28)) // May 29
        val o = CycleRisk.analyze(pred, periods, logs, today = lastStart.plusDays(12))
        assertThat(o.emergencyPillThisCycle).isTrue()
        assertThat(o.delayDays).isEqualTo(CycleRisk.EMERGENCY_PILL_DELAY_DAYS)
        assertThat(o.adjustedNextPeriod).isEqualTo(lastStart.plusDays(28 + 3L)) // June 1
    }

    @Test fun overduePeriodAfterUnprotectedSexFlagsPregnancyChance() {
        val logs = listOf(DayLog(lastStart.plusDays(12), intimacy = Intimacy.UNPROTECTED))
        val pred = prediction(lastStart.plusDays(28)) // May 29
        // 6 days past the predicted period (no EC delay) → LIKELY (suggest a test)
        val o = CycleRisk.analyze(pred, periods, logs, today = lastStart.plusDays(34))
        assertThat(o.unprotectedThisCycle).isTrue()
        assertThat(o.daysLate).isEqualTo(6)
        assertThat(o.pregnancyChance).isEqualTo(CycleRisk.PregnancyChance.LIKELY)
    }

    @Test fun protectedSexOnTimeIsNoConcern() {
        val logs = listOf(DayLog(lastStart.plusDays(12), intimacy = Intimacy.PROTECTED))
        val pred = prediction(lastStart.plusDays(28))
        val o = CycleRisk.analyze(pred, periods, logs, today = lastStart.plusDays(20))
        assertThat(o.pregnancyChance).isEqualTo(CycleRisk.PregnancyChance.NONE)
        assertThat(o.daysLate).isEqualTo(0)
    }

    @Test fun lateButNoRiskEventDoesNotFlag() {
        val logs = emptyList<DayLog>()
        val pred = prediction(lastStart.plusDays(28))
        val o = CycleRisk.analyze(pred, periods, logs, today = lastStart.plusDays(40))
        assertThat(o.pregnancyChance).isEqualTo(CycleRisk.PregnancyChance.NONE)
    }
}
