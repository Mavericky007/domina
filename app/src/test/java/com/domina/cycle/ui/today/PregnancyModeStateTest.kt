package com.domina.cycle.ui.today

import com.domina.cycle.domain.pregnancy.AppMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PregnancyModeStateTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun pregnancyModeWithDueDateProducesProgressAndWeekContent() {
        val s = TodayViewModel.buildPregnancyState(
            mode = AppMode.PREGNANCY, dueDate = d("2026-10-08"), today = d("2026-04-23"))
        assertThat(s).isNotNull()
        assertThat(s!!.progress.weeksCompleted).isEqualTo(16)
        assertThat(s.week.fruit).contains("avocado")
    }

    @Test fun pregnancyModeWithoutDueDateIsNull() {
        assertThat(
            TodayViewModel.buildPregnancyState(AppMode.PREGNANCY, dueDate = null, today = d("2026-04-23"))
        ).isNull()
    }

    @Test fun nonPregnancyModeIsNull() {
        assertThat(
            TodayViewModel.buildPregnancyState(AppMode.CYCLE, dueDate = d("2026-10-08"), today = d("2026-04-23"))
        ).isNull()
    }
}
