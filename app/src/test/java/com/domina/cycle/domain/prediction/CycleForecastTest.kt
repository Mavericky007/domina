package com.domina.cycle.domain.prediction

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.pregnancy.AppMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CycleForecastTest {
    private val periods = listOf(LoggedPeriod(LocalDate.parse("2026-05-16"), 5))
    private val today = LocalDate.parse("2026-05-24")
    private val logs = listOf(DayLog(LocalDate.parse("2026-05-16"), flow = FlowIntensity.MEDIUM))
    private val prediction = CyclePredictor.predict(periods, today)

    @Test fun pregnancyModeProducesNoMarks() {
        val m = CycleForecast.marksFor(AppMode.PREGNANCY, periods, YearMonth.of(2026, 6), 28, 5, today)
        assertThat(m.predictedPeriod).isEmpty()
        assertThat(m.fertile).isEmpty()
        assertThat(m.ovulation).isEmpty()
    }

    @Test fun pregnancyModeOutlookIsEmpty() {
        val o = CycleForecast.outlookFor(AppMode.PREGNANCY, prediction, periods, logs, today)
        assertThat(o).isEqualTo(CycleRisk.Outlook.EMPTY)
    }

    @Test fun cycleModeDelegatesToProjection() {
        val gated = CycleForecast.marksFor(AppMode.CYCLE, periods, YearMonth.of(2026, 6), 28, 5, today)
        val direct = CycleProjection.marksFor(periods, YearMonth.of(2026, 6), 28, 5, today)
        assertThat(gated).isEqualTo(direct)
    }
}
