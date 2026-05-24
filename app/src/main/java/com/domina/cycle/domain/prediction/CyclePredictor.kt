package com.domina.cycle.domain.prediction

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Adaptive cycle predictions derived from logged period history. Pure & deterministic. */
object CyclePredictor {
    const val DEFAULT_CYCLE_LENGTH = 28
    const val DEFAULT_PERIOD_LENGTH = 5
    private const val LUTEAL_LENGTH = 14
    private const val RECENT_WINDOW = 6

    fun predict(periods: List<LoggedPeriod>, today: LocalDate): CyclePrediction {
        if (periods.isEmpty()) {
            return CyclePrediction(
                cycleDay = null, phase = null,
                averageCycleLength = DEFAULT_CYCLE_LENGTH,
                averagePeriodLength = DEFAULT_PERIOD_LENGTH,
                nextPeriodDate = null, ovulationDate = null,
                fertileWindowStart = null, fertileWindowEnd = null,
                confidence = Confidence.NONE,
            )
        }
        val sorted = periods.sortedBy { it.start }
        val lastStart = sorted.last().start

        val gaps = sorted.map { it.start }
            .zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        val recentGaps = gaps.takeLast(RECENT_WINDOW)
        val avgCycle = if (recentGaps.isEmpty()) DEFAULT_CYCLE_LENGTH else recentGaps.average().roundToInt()
        val recentLengths = sorted.takeLast(RECENT_WINDOW).map { it.lengthDays }
        val avgPeriod = if (recentLengths.isEmpty()) DEFAULT_PERIOD_LENGTH else recentLengths.average().roundToInt()

        val cycleDay = if (!today.isBefore(lastStart))
            ChronoUnit.DAYS.between(lastStart, today).toInt() + 1 else null

        val nextPeriod = lastStart.plusDays(avgCycle.toLong())
        val ovulation = nextPeriod.minusDays(LUTEAL_LENGTH.toLong())
        val ovulationDay = avgCycle - LUTEAL_LENGTH

        val phase = cycleDay?.let { phaseFor(it, avgPeriod, ovulationDay) }

        return CyclePrediction(
            cycleDay = cycleDay,
            phase = phase,
            averageCycleLength = avgCycle,
            averagePeriodLength = avgPeriod,
            nextPeriodDate = nextPeriod,
            ovulationDate = ovulation,
            fertileWindowStart = ovulation.minusDays(5),
            fertileWindowEnd = ovulation.plusDays(1),
            confidence = confidenceFor(gaps),
        )
    }

    private fun phaseFor(day: Int, periodLen: Int, ovulationDay: Int): CyclePhase = when {
        day <= periodLen -> CyclePhase.MENSTRUAL
        day in (ovulationDay - 1)..(ovulationDay + 1) -> CyclePhase.OVULATION
        day < ovulationDay - 1 -> CyclePhase.FOLLICULAR
        else -> CyclePhase.LUTEAL
    }

    private fun confidenceFor(gaps: List<Int>): Confidence {
        if (gaps.isEmpty()) return Confidence.LOW
        val recent = gaps.takeLast(RECENT_WINDOW)
        val range = recent.max() - recent.min()
        return when {
            recent.size >= 3 && range <= 4 -> Confidence.HIGH
            range <= 9 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
    }
}
