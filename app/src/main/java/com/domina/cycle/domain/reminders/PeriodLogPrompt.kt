package com.domina.cycle.domain.reminders

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.prediction.CyclePrediction
import com.domina.cycle.domain.prediction.LoggedPeriod
import java.time.LocalDate

/**
 * Decides whether to nudge the user to log her period today. The window opens at the predicted
 * next start and re-anchors to the actual start once a flow day is logged; it runs for an adaptive
 * number of days (her average period length, default 7, clamped 3..10). A day is skipped if its
 * flow is already logged.
 */
object PeriodLogPrompt {
    private const val DEFAULT_LENGTH = 7
    private const val MIN_LENGTH = 3
    private const val MAX_LENGTH = 10
    private const val PREDICTED_GRACE = 3 // tolerate a period arriving a few days late

    data class Window(val start: LocalDate, val endExclusive: LocalDate)

    private fun length(prediction: CyclePrediction): Int =
        prediction.averagePeriodLength.takeIf { it in MIN_LENGTH..MAX_LENGTH } ?: DEFAULT_LENGTH

    fun windowFor(today: LocalDate, periods: List<LoggedPeriod>, prediction: CyclePrediction): Window? {
        val len = length(prediction).toLong()
        val actual = periods.maxByOrNull { it.start }?.start
        if (actual != null && !today.isBefore(actual) && today.isBefore(actual.plusDays(len))) {
            return Window(actual, actual.plusDays(len))
        }
        val predicted = prediction.nextPeriodDate ?: return null
        val end = predicted.plusDays(len + PREDICTED_GRACE)
        if (!today.isBefore(predicted) && today.isBefore(end)) return Window(predicted, end)
        return null
    }

    fun shouldPromptOn(
        today: LocalDate,
        logs: List<DayLog>,
        prediction: CyclePrediction,
        periods: List<LoggedPeriod>,
    ): Boolean {
        val w = windowFor(today, periods, prediction) ?: return false
        if (today.isBefore(w.start) || !today.isBefore(w.endExclusive)) return false
        val loggedToday = logs.any { it.date == today && it.flow != FlowIntensity.NONE }
        return !loggedToday
    }
}
