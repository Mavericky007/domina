package com.domina.cycle.widget

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.pregnancy.AppMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class WidgetDataTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun flow(s: String) = DayLog(date = d(s), flow = FlowIntensity.MEDIUM)

    @Test fun noDataShowsWelcome() {
        val w = WidgetData.build(emptyList(), AppMode.CYCLE, null, d("2026-05-10"))
        assertThat(w.headline).isEqualTo("Domina")
        assertThat(w.subline).contains("Log")
    }

    @Test fun cycleModeShowsDayAndPhase() {
        val logs = listOf(flow("2026-02-01"), flow("2026-03-01"), flow("2026-03-29"), flow("2026-04-26"))
        val w = WidgetData.build(logs, AppMode.CYCLE, null, d("2026-05-05")) // cycle day 10
        assertThat(w.headline).isEqualTo("Cycle Day 10")
        assertThat(w.subline.lowercase()).contains("follicular")
    }

    @Test fun pregnancyModeShowsWeekAndCountdown() {
        val w = WidgetData.build(emptyList(), AppMode.PREGNANCY, d("2026-10-08"), d("2026-04-23"))
        assertThat(w.headline).isEqualTo("Week 16")
        assertThat(w.subline).contains("to go")
    }
}
