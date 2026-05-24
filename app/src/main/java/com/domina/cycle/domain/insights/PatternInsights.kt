package com.domina.cycle.domain.insights

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.domain.prediction.CyclePhase
import com.domina.cycle.domain.prediction.LoggedPeriod
import java.time.temporal.ChronoUnit

object PatternInsights {
    private const val LUTEAL = 14

    fun generate(logs: List<DayLog>, periods: List<LoggedPeriod>, averageCycle: Int): List<String> {
        if (logs.isEmpty() || periods.isEmpty()) return emptyList()
        val starts = periods.map { it.start }.sorted()
        val out = mutableListOf<String>()

        // symptom -> phase tally
        val tally = HashMap<String, HashMap<CyclePhase, Int>>()
        logs.forEach { log ->
            val start = starts.lastOrNull { !it.isAfter(log.date) } ?: return@forEach
            val day = ChronoUnit.DAYS.between(start, log.date).toInt() + 1
            val phase = phaseOf(day, averageCycle)
            log.symptoms.forEach { sym ->
                tally.getOrPut(sym) { HashMap() }.merge(phase, 1, Int::plus)
            }
        }
        tally.forEach { (sym, phases) ->
            val total = phases.values.sum()
            val (topPhase, topCount) = phases.maxByOrNull { it.value }!!
            if (total >= 3 && topCount * 2 > total) { // clear majority over >=3 occurrences
                out += "$sym tend to show up in your ${topPhase.label()} phase 💛"
            }
        }
        return out
    }

    private fun phaseOf(day: Int, cycle: Int): CyclePhase {
        val ovulationDay = cycle - LUTEAL
        return when {
            day <= 5 -> CyclePhase.MENSTRUAL
            day in (ovulationDay - 1)..(ovulationDay + 1) -> CyclePhase.OVULATION
            day < ovulationDay - 1 -> CyclePhase.FOLLICULAR
            else -> CyclePhase.LUTEAL
        }
    }

    private fun CyclePhase.label() = name.lowercase()
}
