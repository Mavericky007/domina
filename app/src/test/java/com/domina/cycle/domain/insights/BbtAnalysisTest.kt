package com.domina.cycle.domain.insights

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BbtAnalysisTest {
    @Test fun noRiseReturnsNull() {
        val flat = List(10) { 36.4 }
        val r = BbtAnalysis.detectCoverline(flat)
        assertThat(r.coverline).isNull()
        assertThat(r.riseIndex).isNull()
    }

    @Test fun detectsThreeOverSixShift() {
        // 6 low temps, then a sustained rise
        val temps = listOf(36.4, 36.3, 36.4, 36.5, 36.4, 36.3, 36.7, 36.8, 36.75, 36.7)
        val r = BbtAnalysis.detectCoverline(temps)
        assertThat(r.riseIndex).isEqualTo(6)
        assertThat(r.coverline!!).isWithin(0.001).of(36.6) // max(prev6)=36.5, +0.1
    }

    @Test fun tooFewPointsReturnsNull() {
        assertThat(BbtAnalysis.detectCoverline(listOf(36.4, 36.5)).coverline).isNull()
    }
}
