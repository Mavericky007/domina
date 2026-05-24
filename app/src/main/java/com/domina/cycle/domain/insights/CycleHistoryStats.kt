package com.domina.cycle.domain.insights

import com.domina.cycle.domain.prediction.LoggedPeriod
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object CycleHistoryStats {
    data class History(val cycleLengths: List<Int>, val average: Int, val shortest: Int, val longest: Int)

    fun compute(periods: List<LoggedPeriod>): History {
        val starts = periods.sortedBy { it.start }.map { it.start }
        val lengths = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        if (lengths.isEmpty()) return History(emptyList(), 0, 0, 0)
        return History(lengths, lengths.average().roundToInt(), lengths.min(), lengths.max())
    }
}
