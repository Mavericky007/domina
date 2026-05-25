package com.domina.cycle.domain.prediction

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CycleProjectionTest {
    private val periods = listOf(LoggedPeriod(LocalDate.parse("2026-05-16"), 5))
    private val today = LocalDate.parse("2026-05-24")

    @Test fun nextMonthShowsPredictedPeriodAndOvulation() {
        val m = CycleProjection.marksFor(periods, YearMonth.of(2026, 6), avgCycle = 28, avgPeriod = 5, today = today)
        // next period: May 16 + 28 = June 13, lasting 5 days
        assertThat(m.predictedPeriod).containsExactly(13, 14, 15, 16, 17)
        // ovulation = June 13 + 14 = June 27
        assertThat(m.ovulation).containsExactly(27)
        assertThat(m.fertile).contains(24) // within June 22..28 window (minus ovulation)
    }

    @Test fun currentMonthShowsUpcomingFertileNotPastPeriod() {
        val m = CycleProjection.marksFor(periods, YearMonth.of(2026, 5), avgCycle = 28, avgPeriod = 5, today = today)
        assertThat(m.predictedPeriod).isEmpty()      // May 16–20 period is in the past
        assertThat(m.ovulation).containsExactly(30)  // May 16 + 14 = May 30
        assertThat(m.fertile).contains(25)
    }

    @Test fun emptyHistoryMarksNothing() {
        val m = CycleProjection.marksFor(emptyList(), YearMonth.of(2026, 6), 28, 5, today)
        assertThat(m.predictedPeriod).isEmpty()
        assertThat(m.fertile).isEmpty()
        assertThat(m.ovulation).isEmpty()
    }
}
