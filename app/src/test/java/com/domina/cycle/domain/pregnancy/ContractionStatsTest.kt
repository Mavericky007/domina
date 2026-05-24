package com.domina.cycle.domain.pregnancy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContractionStatsTest {
    // each pair is (startMillis, endMillis)
    private fun c(start: Long, end: Long) = ContractionStats.Contraction(start, end)

    @Test fun emptyHasZeroStats() {
        val s = ContractionStats.compute(emptyList())
        assertThat(s.count).isEqualTo(0)
        assertThat(s.averageDurationSec).isEqualTo(0)
        assertThat(s.averageIntervalSec).isEqualTo(0)
    }

    @Test fun computesAverageDurationAndInterval() {
        // contractions starting at 0s, 300s, 600s; each 60s long
        val list = listOf(c(0, 60_000), c(300_000, 360_000), c(600_000, 660_000))
        val s = ContractionStats.compute(list)
        assertThat(s.count).isEqualTo(3)
        assertThat(s.averageDurationSec).isEqualTo(60)        // each 60s
        assertThat(s.averageIntervalSec).isEqualTo(300)       // 5 minutes apart
    }

    @Test fun handlesUnsortedInput() {
        val list = listOf(c(600_000, 660_000), c(0, 60_000), c(300_000, 360_000))
        assertThat(ContractionStats.compute(list).averageIntervalSec).isEqualTo(300)
    }
}
