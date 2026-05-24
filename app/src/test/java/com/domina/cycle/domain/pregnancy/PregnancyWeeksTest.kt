package com.domina.cycle.domain.pregnancy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PregnancyWeeksTest {
    @Test fun coversWeeks4Through40Contiguously() {
        val weeks = PregnancyWeeks.all.map { it.week }
        assertThat(weeks).isEqualTo((4..40).toList())
    }

    @Test fun everyWeekHasContent() {
        PregnancyWeeks.all.forEach { w ->
            assertThat(w.fruit).isNotEmpty()
            assertThat(w.emoji).isNotEmpty()
            assertThat(w.development).isNotEmpty()
            assertThat(w.funFact).isNotEmpty()
            assertThat(w.lengthCm).isAtLeast(0.0)
        }
    }

    @Test fun forWeekReturnsExactMatch() {
        assertThat(PregnancyWeeks.forWeek(16).fruit).contains("avocado")
    }

    @Test fun forWeekClampsOutOfRange() {
        assertThat(PregnancyWeeks.forWeek(1).week).isEqualTo(4)   // below range -> first
        assertThat(PregnancyWeeks.forWeek(99).week).isEqualTo(40) // above range -> last
    }
}
