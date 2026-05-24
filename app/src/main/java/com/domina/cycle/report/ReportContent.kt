package com.domina.cycle.report

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.insights.CycleHistoryStats
import com.domina.cycle.domain.prediction.PeriodDeriver
import java.time.LocalDate

data class ReportSection(val title: String, val lines: List<String>)
data class Report(val title: String, val generatedOn: LocalDate, val sections: List<ReportSection>)

object ReportContent {
    fun build(logs: List<DayLog>, generatedOn: LocalDate = LocalDate.now()): Report {
        val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
        val history = CycleHistoryStats.compute(periods)
        val sections = mutableListOf<ReportSection>()

        sections += ReportSection(
            "Cycle summary",
            if (history.cycleLengths.isEmpty()) listOf("Not enough cycle data logged yet.")
            else listOf(
                "Cycles recorded: ${history.cycleLengths.size + 1}",
                "Average cycle length: ${history.average} days",
                "Range: ${history.shortest}–${history.longest} days",
            ),
        )

        val recentStarts = periods.takeLast(6).map { "Period started ${it.start} (lasted ${it.lengthDays} days)" }
        sections += ReportSection("Recent periods",
            recentStarts.ifEmpty { listOf("No periods logged yet.") })

        val recentSymptoms = logs.filter { it.symptoms.isNotEmpty() }.sortedByDescending { it.date }.take(15)
            .map { "${it.date}: ${it.symptoms.joinToString(", ")}" }
        sections += ReportSection("Recent symptoms",
            recentSymptoms.ifEmpty { listOf("No symptoms logged yet.") })

        val bbt = logs.filter { it.bbt != null }.sortedBy { it.date }
        sections += ReportSection("Basal body temperature",
            if (bbt.isEmpty()) listOf("No BBT readings logged yet.")
            else listOf("${bbt.size} readings, latest ${bbt.last().bbt} °C on ${bbt.last().date}"))

        return Report("Domina — Cycle Report", generatedOn, sections)
    }
}
