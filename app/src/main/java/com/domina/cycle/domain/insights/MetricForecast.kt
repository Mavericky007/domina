package com.domina.cycle.domain.insights

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Projects a body metric (BBT, weight, …) forward across the predicted cycle.
 *
 * Model = average value per cycle phase. BBT sits higher in the luteal phase (after ovulation)
 * than in the follicular phase, so the forecast steps up/down as the predicted ovulation and next
 * period pass — surfacing the cyclical fluctuation without inventing a spurious long-term trend.
 */
object MetricForecast {
    private const val LUTEAL_LENGTH = 14

    data class Sample(val date: LocalDate, val value: Float)

    /**
     * @param history    chronologically sorted observed samples
     * @param actualStarts observed period start dates (for classifying historical phase)
     * @param avgCycle   average cycle length (days)
     * @param horizon    how many days past the last sample to project
     */
    fun forecast(
        history: List<Sample>,
        actualStarts: List<LocalDate>,
        avgCycle: Int,
        horizon: Int,
    ): List<Sample> {
        if (history.size < 2 || horizon <= 0) return emptyList()

        val overall = history.map { it.value }.average().toFloat()

        val ovOffset = (avgCycle - LUTEAL_LENGTH).coerceAtLeast(1)
        fun isLuteal(d: LocalDate, starts: List<LocalDate>): Boolean? {
            val s = starts.lastOrNull { !it.isAfter(d) } ?: return null
            return ChronoUnit.DAYS.between(s, d) >= ovOffset
        }

        // ── average value per phase (fall back to overall when a phase has no data) ──
        val foll = ArrayList<Float>(); val lut = ArrayList<Float>()
        history.forEach { s ->
            when (isLuteal(s.date, actualStarts)) {
                true -> lut.add(s.value)
                false -> foll.add(s.value)
                null -> {}
            }
        }
        val follMean = if (foll.isEmpty()) overall else foll.average().toFloat()
        val lutMean = if (lut.isEmpty()) overall else lut.average().toFloat()

        // ── project predicted period starts to classify future phases ──
        val lastDate = history.last().date
        val end = lastDate.plusDays(horizon.toLong())
        val predicted = actualStarts.maxOrNull()?.let { last ->
            generateSequence(last) { it.plusDays(avgCycle.toLong()) }.takeWhile { !it.isAfter(end) }.toList()
        } ?: emptyList()
        val allStarts = (actualStarts + predicted).distinct().sorted()

        val out = ArrayList<Sample>()
        var d = lastDate.plusDays(1)
        while (!d.isAfter(end)) {
            val luteal = isLuteal(d, allStarts) ?: false
            out.add(Sample(d, if (luteal) lutMean else follMean))
            d = d.plusDays(1)
        }
        return out
    }
}
