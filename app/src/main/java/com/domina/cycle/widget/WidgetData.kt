package com.domina.cycle.widget

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.pregnancy.AppMode
import com.domina.cycle.domain.pregnancy.PregnancyCalculator
import com.domina.cycle.domain.prediction.Confidence
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WidgetInfo(val headline: String, val subline: String)

object WidgetData {
    fun build(logs: List<DayLog>, mode: AppMode, dueDate: LocalDate?, today: LocalDate): WidgetInfo {
        if (mode == AppMode.PREGNANCY && dueDate != null) {
            val p = PregnancyCalculator.progress(dueDate, today)
            val sub = if (p.daysRemaining >= 0) "${p.daysRemaining} days to go" else "due any moment"
            return WidgetInfo("Week ${p.weeksCompleted}", sub)
        }
        val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
        val pred = CyclePredictor.predict(periods, today)
        if (pred.confidence == Confidence.NONE || pred.cycleDay == null) {
            return WidgetInfo("Domina", "Log a period to begin")
        }
        val phase = pred.phase?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
        val days = pred.nextPeriodDate?.let { ChronoUnit.DAYS.between(today, it).toInt() }
        val sub = when {
            days == null -> phase
            days <= 0 -> "$phase · period due"
            else -> "$phase · period in ${days}d"
        }
        return WidgetInfo("Cycle Day ${pred.cycleDay}", sub)
    }
}
