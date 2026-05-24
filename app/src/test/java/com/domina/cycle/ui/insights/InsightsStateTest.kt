package com.domina.cycle.ui.insights

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.db.entity.WeightEntryEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class InsightsStateTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun flow(date: String) = DayLog(date = d(date), flow = FlowIntensity.MEDIUM)

    @Test fun buildsCycleLengthsAndWeightSeriesFromData() {
        val logs = listOf(flow("2026-02-01"), flow("2026-03-01"), flow("2026-03-29"))
        val weights = listOf(
            WeightEntryEntity(dateEpochDay = d("2026-05-01").toEpochDay(), weightKg = 64.0),
            WeightEntryEntity(dateEpochDay = d("2026-05-15").toEpochDay(), weightKg = 65.0),
        )
        val s = InsightsViewModel.buildState(logs, weights)
        assertThat(s.cycleLengths).containsExactly(28, 28).inOrder()
        assertThat(s.weightSeries).containsExactly(64.0f, 65.0f).inOrder()
    }
}
