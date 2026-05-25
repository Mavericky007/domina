package com.domina.cycle.domain.prediction

import java.time.LocalDate
import java.time.YearMonth

/**
 * Projects upcoming predicted period / fertile-window / ovulation days onto a calendar month,
 * based on logged history. Only marks days on or after [today] (past days come from real logs).
 */
object CycleProjection {
    data class MonthMarks(
        val predictedPeriod: Set<Int> = emptySet(),
        val fertile: Set<Int> = emptySet(),
        val ovulation: Set<Int> = emptySet(),
    )

    private const val LUTEAL_LENGTH = 14

    fun marksFor(
        periods: List<LoggedPeriod>,
        month: YearMonth,
        avgCycle: Int,
        avgPeriod: Int,
        today: LocalDate,
        delayDays: Int = 0,   // shifts upcoming predictions later (e.g. after a morning-after pill)
    ): MonthMarks {
        if (periods.isEmpty() || avgCycle <= 0) return MonthMarks()
        val lastStart = periods.maxOf { it.start }
        val predicted = sortedSetOf<Int>()
        val fertile = sortedSetOf<Int>()
        val ovulation = sortedSetOf<Int>()
        val monthEnd = month.atEndOfMonth()

        fun mark(rawDay: LocalDate, into: MutableSet<Int>) {
            val day = rawDay.plusDays(delayDays.toLong())
            if (YearMonth.from(day) == month && !day.isBefore(today)) into.add(day.dayOfMonth)
        }

        var start = lastStart
        var guard = 0
        while (!start.isAfter(monthEnd) && guard < 48) {
            for (d in 0 until avgPeriod) mark(start.plusDays(d.toLong()), predicted)
            val ov = start.plusDays((avgCycle - LUTEAL_LENGTH).toLong())
            for (d in -5..1) mark(ov.plusDays(d.toLong()), fertile)
            mark(ov, ovulation)
            start = start.plusDays(avgCycle.toLong())
            guard++
        }
        return MonthMarks(predicted, fertile - ovulation, ovulation)
    }
}
