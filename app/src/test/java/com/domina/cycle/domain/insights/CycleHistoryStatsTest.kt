package com.domina.cycle.domain.insights

import com.domina.cycle.domain.prediction.LoggedPeriod
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class CycleHistoryStatsTest {
    private fun p(s: String) = LoggedPeriod(LocalDate.parse(s), 5)

    @Test fun emptyOrSingleHasNoCycleLengths() {
        assertThat(CycleHistoryStats.compute(emptyList()).cycleLengths).isEmpty()
        assertThat(CycleHistoryStats.compute(listOf(p("2026-05-01"))).cycleLengths).isEmpty()
    }

    @Test fun computesLengthsAndStats() {
        val periods = listOf(p("2026-02-01"), p("2026-03-01"), p("2026-03-29"), p("2026-04-30"))
        val s = CycleHistoryStats.compute(periods) // gaps: 28, 28, 32
        assertThat(s.cycleLengths).containsExactly(28, 28, 32).inOrder()
        assertThat(s.average).isEqualTo(29)   // (28+28+32)/3 = 29.33 -> 29
        assertThat(s.shortest).isEqualTo(28)
        assertThat(s.longest).isEqualTo(32)
    }
}
