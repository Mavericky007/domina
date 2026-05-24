package com.domina.cycle.ui.today

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.prediction.CyclePhase
import com.domina.cycle.domain.prediction.Confidence
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class TodayUiStateTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun flowLog(date: String) = DayLog(date = d(date), flow = FlowIntensity.MEDIUM)

    @Test fun noFlowLogsYieldNoneConfidenceAndNoGuidance() {
        val state = TodayViewModel.buildState(emptyList(), d("2026-05-10"))
        assertThat(state.prediction.confidence).isEqualTo(Confidence.NONE)
        assertThat(state.guidance).isNull()
    }

    @Test fun derivesPredictionAndGuidanceFromFlowLogs() {
        // period starts on the 1st of four consecutive months (~monthly), today = day ~10
        val logs = listOf(
            flowLog("2026-02-01"), flowLog("2026-03-01"),
            flowLog("2026-03-29"), flowLog("2026-04-26"),
        )
        val state = TodayViewModel.buildState(logs, d("2026-05-05"))
        assertThat(state.prediction.cycleDay).isEqualTo(10)
        assertThat(state.prediction.phase).isEqualTo(CyclePhase.FOLLICULAR)
        assertThat(state.guidance).isNotNull()
        assertThat(state.guidance!!.phase).isEqualTo(CyclePhase.FOLLICULAR)
    }

    @Test fun exposesTodaysLogWhenPresent() {
        val logs = listOf(flowLog("2026-05-05").copy(date = d("2026-05-10")))
        val state = TodayViewModel.buildState(logs, d("2026-05-10"))
        assertThat(state.todayLog?.date).isEqualTo(d("2026-05-10"))
    }
}
