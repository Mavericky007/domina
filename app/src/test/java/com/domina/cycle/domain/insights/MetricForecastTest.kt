package com.domina.cycle.domain.insights

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class MetricForecastTest {

    @Test fun biphasicTemperatureProjectsHigherInLutealThanFollicular() {
        val start = LocalDate.parse("2026-01-01")   // cycle day 0 = period start
        // 28-day cycle, ovulation ~ day 14. Follicular ≈ 36.4, luteal ≈ 36.7.
        val history = (0..27).map { day ->
            val date = start.plusDays(day.toLong())
            val v = if (day >= 14) 36.7f else 36.4f
            MetricForecast.Sample(date, v)
        }
        val forecast = MetricForecast.forecast(
            history = history,
            actualStarts = listOf(start),
            avgCycle = 28,
            horizon = 28,
        )
        assertThat(forecast).isNotEmpty()
        // First projected day is day 28 = next period start → follicular (low).
        val firstFollicular = forecast.first().value
        // A later luteal day (e.g. day 28 + 16 = day 44) should be clearly higher.
        val lateLuteal = forecast.first { it.date >= start.plusDays(44) }.value
        assertThat(lateLuteal).isGreaterThan(firstFollicular)
        assertThat(lateLuteal - firstFollicular).isWithin(0.15f).of(0.3f)
    }

    @Test fun emptyOrTinyHistoryProjectsNothing() {
        assertThat(MetricForecast.forecast(emptyList(), emptyList(), 28, 28)).isEmpty()
        assertThat(
            MetricForecast.forecast(
                listOf(MetricForecast.Sample(LocalDate.parse("2026-01-01"), 60f)),
                emptyList(), 28, 28,
            ),
        ).isEmpty()
    }
}
