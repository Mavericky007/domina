package com.domina.cycle.domain.prediction

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PeriodDeriverTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun emptyInputGivesNoPeriods() {
        assertThat(PeriodDeriver.derive(emptyList())).isEmpty()
    }

    @Test fun singleFlowDayIsOnePeriodOfLengthOne() {
        val r = PeriodDeriver.derive(listOf(d("2026-05-01")))
        assertThat(r).containsExactly(LoggedPeriod(d("2026-05-01"), 1))
    }

    @Test fun consecutiveDaysCollapseIntoOnePeriod() {
        val r = PeriodDeriver.derive(listOf(d("2026-05-01"), d("2026-05-02"), d("2026-05-03")))
        assertThat(r).containsExactly(LoggedPeriod(d("2026-05-01"), 3))
    }

    @Test fun gapStartsANewPeriodAndInputIsSortedAndDeduped() {
        val r = PeriodDeriver.derive(
            listOf(d("2026-05-02"), d("2026-05-01"), d("2026-05-01"), d("2026-05-29"), d("2026-05-30"))
        )
        assertThat(r).containsExactly(
            LoggedPeriod(d("2026-05-01"), 2),
            LoggedPeriod(d("2026-05-29"), 2),
        ).inOrder()
    }
}
