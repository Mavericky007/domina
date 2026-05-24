package com.domina.cycle.ui.calendar

import java.time.DayOfWeek
import java.time.YearMonth

/** Month grid as cells (null = blank), Monday-start, padded to whole weeks. */
object CalendarMonth {
    val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    fun cells(month: YearMonth): List<Int?> {
        val leading = (month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val days = (1..month.lengthOfMonth()).toList()
        val out = ArrayList<Int?>(leading + days.size)
        repeat(leading) { out.add(null) }
        out.addAll(days)
        while (out.size % 7 != 0) out.add(null)
        return out
    }
}
