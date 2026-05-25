package com.domina.cycle.domain.pregnancy

import java.time.LocalDate
import java.time.YearMonth

/** Maps each day of [month] that falls within the pregnancy span to its trimester (1, 2 or 3). */
object PregnancyProjection {
    private const val TERM_DAYS = 280L

    fun marksFor(dueDate: LocalDate, month: YearMonth): Map<Int, Int> {
        val start = dueDate.minusDays(TERM_DAYS) // LMP / week 0
        val result = HashMap<Int, Int>()
        var day = month.atDay(1)
        val end = month.atEndOfMonth()
        while (!day.isAfter(end)) {
            if (!day.isBefore(start) && !day.isAfter(dueDate)) {
                val weeksCompleted = (java.time.temporal.ChronoUnit.DAYS.between(start, day) / 7).toInt()
                result[day.dayOfMonth] = when {
                    weeksCompleted <= 13 -> 1
                    weeksCompleted <= 27 -> 2
                    else -> 3
                }
            }
            day = day.plusDays(1)
        }
        return result
    }
}
