package com.domina.cycle.domain.prediction

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class CyclePredictorTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun starts(vararg s: String, len: Int = 5) = s.map { LoggedPeriod(d(it), len) }

    @Test fun noDataYieldsNoneConfidenceAndDefaults() {
        val p = CyclePredictor.predict(emptyList(), d("2026-05-10"))
        assertThat(p.confidence).isEqualTo(Confidence.NONE)
        assertThat(p.cycleDay).isNull()
        assertThat(p.phase).isNull()
        assertThat(p.averageCycleLength).isEqualTo(28)
        assertThat(p.nextPeriodDate).isNull()
    }

    @Test fun singlePeriodUsesDefaultsAndLowConfidence() {
        val p = CyclePredictor.predict(starts("2026-05-01"), d("2026-05-03"))
        assertThat(p.confidence).isEqualTo(Confidence.LOW)
        assertThat(p.cycleDay).isEqualTo(3)              // May 1 -> May 3 = day 3
        assertThat(p.averageCycleLength).isEqualTo(28)
        assertThat(p.nextPeriodDate).isEqualTo(d("2026-05-29"))
        assertThat(p.phase).isEqualTo(CyclePhase.MENSTRUAL) // day 3 <= 5
    }

    @Test fun regularCyclesGiveHighConfidenceAndCorrectAverages() {
        // four starts exactly 28 apart
        val periods = starts("2026-02-01", "2026-03-01", "2026-03-29", "2026-04-26")
        val p = CyclePredictor.predict(periods, d("2026-05-05")) // day 10 of current cycle
        assertThat(p.averageCycleLength).isEqualTo(28)
        assertThat(p.confidence).isEqualTo(Confidence.HIGH)
        assertThat(p.cycleDay).isEqualTo(10)
        assertThat(p.phase).isEqualTo(CyclePhase.FOLLICULAR) // 6..12
        assertThat(p.nextPeriodDate).isEqualTo(d("2026-05-24"))
        assertThat(p.ovulationDate).isEqualTo(d("2026-05-10")) // next - 14
        assertThat(p.fertileWindowStart).isEqualTo(d("2026-05-05"))
        assertThat(p.fertileWindowEnd).isEqualTo(d("2026-05-11"))
    }

    @Test fun phaseBoundariesForA28DayCycle() {
        val periods = starts("2026-02-01", "2026-03-01", "2026-03-29", "2026-04-26")
        fun phaseOnDay(day: Int) =
            CyclePredictor.predict(periods, d("2026-04-26").plusDays((day - 1).toLong())).phase
        assertThat(phaseOnDay(3)).isEqualTo(CyclePhase.MENSTRUAL)   // 1..5
        assertThat(phaseOnDay(9)).isEqualTo(CyclePhase.FOLLICULAR)  // 6..12
        assertThat(phaseOnDay(14)).isEqualTo(CyclePhase.OVULATION)  // 13..15
        assertThat(phaseOnDay(20)).isEqualTo(CyclePhase.LUTEAL)     // 16..28
    }

    @Test fun irregularCyclesGiveMediumOrLowNotHigh() {
        val periods = starts("2026-01-01", "2026-01-26", "2026-03-02", "2026-03-28") // gaps 25,35,26 -> range 10
        val p = CyclePredictor.predict(periods, d("2026-04-02"))
        assertThat(p.confidence).isEqualTo(Confidence.LOW) // range 10 > 9
    }

    @Test fun latePeriodIsLutealWithDayPastCycleLength() {
        val periods = starts("2026-02-01", "2026-03-01", "2026-03-29", "2026-04-26")
        val p = CyclePredictor.predict(periods, d("2026-05-30")) // day 35, past 28
        assertThat(p.cycleDay).isEqualTo(35)
        assertThat(p.phase).isEqualTo(CyclePhase.LUTEAL)
    }
}
