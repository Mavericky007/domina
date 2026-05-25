package com.domina.cycle.domain.reminders

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.LoggedPeriod
import com.domina.cycle.domain.prediction.PeriodDeriver
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PeriodLogPromptTest {
    private fun history(starts: List<String>, len: Int = 5): Pair<List<DayLog>, List<LoggedPeriod>> {
        val logs = starts.flatMap { s ->
            val d = LocalDate.parse(s)
            (0 until len).map { DayLog(d.plusDays(it.toLong()), flow = FlowIntensity.MEDIUM) }
        }
        val periods = PeriodDeriver.derive(logs.map { it.date })
        return logs to periods
    }

    @Test fun promptsOnPredictedStartWhenNothingLoggedYet() {
        val (logs, periods) = history(listOf("2026-01-01", "2026-01-29", "2026-02-26", "2026-03-26"))
        val today = LocalDate.parse("2026-04-23")
        val pred = CyclePredictor.predict(periods, today)
        assertThat(PeriodLogPrompt.shouldPromptOn(today, logs, pred, periods)).isTrue()
    }

    @Test fun skipsWhenTodayAlreadyLogged() {
        val (logs0, _) = history(listOf("2026-01-01", "2026-01-29", "2026-02-26"))
        val today = LocalDate.parse("2026-03-26")
        val logs = logs0 + DayLog(today, flow = FlowIntensity.LIGHT)
        val periods = PeriodDeriver.derive(logs.map { it.date })
        val pred = CyclePredictor.predict(periods, today)
        assertThat(PeriodLogPrompt.shouldPromptOn(today, logs, pred, periods)).isFalse()
    }

    @Test fun outOfWindowDoesNotPrompt() {
        val (logs, periods) = history(listOf("2026-01-01", "2026-01-29", "2026-02-26"))
        val today = LocalDate.parse("2026-03-10")
        val pred = CyclePredictor.predict(periods, today)
        assertThat(PeriodLogPrompt.shouldPromptOn(today, logs, pred, periods)).isFalse()
    }
}
