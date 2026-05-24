package com.domina.cycle.domain.insights

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.domain.prediction.LoggedPeriod
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PatternInsightsTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun emptyDataGivesNoInsights() {
        assertThat(PatternInsights.generate(emptyList(), emptyList(), 28)).isEmpty()
    }

    @Test fun symptomConcentratedInMenstrualPhaseIsReported() {
        // period started May 1; cramps logged on days 1-3 (menstrual phase)
        val periods = listOf(LoggedPeriod(d("2026-05-01"), 5))
        val logs = listOf(
            DayLog(date = d("2026-05-01"), symptoms = listOf("cramps")),
            DayLog(date = d("2026-05-02"), symptoms = listOf("cramps")),
            DayLog(date = d("2026-05-03"), symptoms = listOf("cramps")),
        )
        val insights = PatternInsights.generate(logs, periods, 28)
        assertThat(insights.any { it.contains("cramps") && it.contains("menstrual", ignoreCase = true) }).isTrue()
    }
}
