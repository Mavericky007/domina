package com.domina.cycle.report

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ReportContentTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun buildsSummaryWithCycleStatsAndRecentPeriods() {
        val logs = listOf(
            DayLog(date = d("2026-02-01"), flow = FlowIntensity.MEDIUM),
            DayLog(date = d("2026-03-01"), flow = FlowIntensity.MEDIUM),
            DayLog(date = d("2026-03-29"), flow = FlowIntensity.MEDIUM, symptoms = listOf("cramps")),
        )
        val report = ReportContent.build(logs, generatedOn = d("2026-04-01"))
        assertThat(report.title).contains("Cycle Report")
        val allText = report.sections.joinToString("\n") { it.title + "\n" + it.lines.joinToString("\n") }
        assertThat(allText).contains("Average cycle")   // cycle-stats section
        assertThat(allText).contains("2026-03-29")      // a recent period start appears
    }

    @Test fun emptyDataStillProducesAReport() {
        val report = ReportContent.build(emptyList(), generatedOn = d("2026-04-01"))
        assertThat(report.sections).isNotEmpty()
    }
}
