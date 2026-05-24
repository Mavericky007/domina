package com.domina.cycle.domain.insights

object BbtAnalysis {
    data class Coverline(val coverline: Double?, val riseIndex: Int?)

    private const val OFFSET = 0.1

    /** Classic 3-over-6 fertility-awareness coverline detection. Temps in cycle (chronological) order. */
    fun detectCoverline(temps: List<Double>): Coverline {
        for (i in 6..temps.size - 3) {
            val baselineMax = temps.subList(i - 6, i).max()
            val coverline = baselineMax + OFFSET
            if (temps[i] > coverline && temps[i + 1] > coverline && temps[i + 2] > coverline) {
                return Coverline(coverline, i)
            }
        }
        return Coverline(null, null)
    }
}
