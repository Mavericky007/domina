package com.domina.cycle.domain.guidance

import com.domina.cycle.domain.prediction.CyclePhase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PhaseGuideTest {
    @Test fun everyPhaseHasCompleteNonBlankGuidance() {
        CyclePhase.entries.forEach { phase ->
            val g = PhaseGuide.forPhase(phase)
            assertThat(g.phase).isEqualTo(phase)
            assertThat(g.title).isNotEmpty()
            assertThat(g.moodForecast).isNotEmpty()
            assertThat(g.move).isNotEmpty()
            assertThat(g.eat).isNotEmpty()
            assertThat(g.plan).isNotEmpty()
        }
    }
}
