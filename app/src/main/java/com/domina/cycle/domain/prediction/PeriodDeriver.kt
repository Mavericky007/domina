package com.domina.cycle.domain.prediction

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Groups runs of consecutive flow days into discrete [LoggedPeriod]s. */
object PeriodDeriver {
    fun derive(flowDates: List<LocalDate>): List<LoggedPeriod> {
        if (flowDates.isEmpty()) return emptyList()
        val sorted = flowDates.distinct().sorted()
        val periods = mutableListOf<LoggedPeriod>()
        var start = sorted.first()
        var prev = sorted.first()
        var length = 1
        for (date in sorted.drop(1)) {
            if (ChronoUnit.DAYS.between(prev, date) == 1L) {
                length++
            } else {
                periods.add(LoggedPeriod(start, length))
                start = date
                length = 1
            }
            prev = date
        }
        periods.add(LoggedPeriod(start, length))
        return periods
    }
}
