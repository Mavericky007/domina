package com.domina.cycle.domain.prediction

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.Intimacy
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Layers contraception & intercourse context on top of a base [CyclePrediction]:
 *  - a morning-after pill (emergency contraception) delays ovulation, so the next period is
 *    expected a few days later than usual (levonorgestrel typically pushes it back up to ~a week;
 *    we shift the estimate by [EMERGENCY_PILL_DELAY_DAYS] and widen expectations via messaging).
 *  - if the period is overdue after unprotected intercourse (or an EC event) this cycle, surface a
 *    pregnancy-awareness signal — nudging toward a test once it's clearly late.
 *
 * Pure & deterministic so it can be unit-tested and shared across Home/Calendar.
 */
object CycleRisk {
    const val EMERGENCY_PILL_DELAY_DAYS = 3
    private const val POSSIBLE_LATE_DAYS = 3
    private const val LIKELY_LATE_DAYS = 5

    enum class PregnancyChance { NONE, POSSIBLE, LIKELY }

    data class Outlook(
        val emergencyPillThisCycle: Boolean,
        val delayDays: Int,                  // EC delay folded into the prediction (0 or EMERGENCY_PILL_DELAY_DAYS)
        val adjustedNextPeriod: LocalDate?,  // base next period + delayDays
        val unprotectedThisCycle: Boolean,
        val daysLate: Int,                   // days past adjustedNextPeriod (0 if not late / unknown)
        val pregnancyChance: PregnancyChance,
    ) {
        companion object {
            val EMPTY = Outlook(false, 0, null, false, 0, PregnancyChance.NONE)
        }
    }

    fun analyze(
        prediction: CyclePrediction,
        periods: List<LoggedPeriod>,
        logs: List<DayLog>,
        today: LocalDate,
    ): Outlook {
        val base = prediction.nextPeriodDate
        val lastStart = periods.maxByOrNull { it.start }?.start ?: return Outlook.EMPTY

        // Logs belonging to the current (in-progress) cycle: from this period's start up to today.
        val cycleLogs = logs.filter { !it.date.isBefore(lastStart) && !it.date.isAfter(today) }
        val emergencyPill = cycleLogs.any { it.emergencyContraception }
        val unprotected = cycleLogs.any { it.intimacy == Intimacy.UNPROTECTED }

        val delay = if (emergencyPill) EMERGENCY_PILL_DELAY_DAYS else 0
        val adjusted = base?.plusDays(delay.toLong())

        val daysLate = if (adjusted != null && today.isAfter(adjusted))
            ChronoUnit.DAYS.between(adjusted, today).toInt() else 0

        val riskEvent = unprotected || emergencyPill
        val chance = when {
            riskEvent && daysLate >= LIKELY_LATE_DAYS -> PregnancyChance.LIKELY
            riskEvent && daysLate >= POSSIBLE_LATE_DAYS -> PregnancyChance.POSSIBLE
            else -> PregnancyChance.NONE
        }

        return Outlook(emergencyPill, delay, adjusted, unprotected, daysLate, chance)
    }
}
