package com.domina.cycle.domain.pregnancy

object ContractionStats {
    data class Contraction(val startMillis: Long, val endMillis: Long)
    data class Stats(val count: Int, val averageDurationSec: Int, val averageIntervalSec: Int)

    fun compute(contractions: List<Contraction>): Stats {
        if (contractions.isEmpty()) return Stats(0, 0, 0)
        val sorted = contractions.sortedBy { it.startMillis }
        val avgDuration = sorted.map { (it.endMillis - it.startMillis) / 1000.0 }.average()
        val intervals = sorted.map { it.startMillis }
            .zipWithNext { a, b -> (b - a) / 1000.0 }
        val avgInterval = if (intervals.isEmpty()) 0.0 else intervals.average()
        return Stats(sorted.size, Math.round(avgDuration).toInt(), Math.round(avgInterval).toInt())
    }
}
