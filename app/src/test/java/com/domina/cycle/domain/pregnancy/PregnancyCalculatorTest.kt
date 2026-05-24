package com.domina.cycle.domain.pregnancy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PregnancyCalculatorTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun dueDateIs280DaysAfterLmp() {
        assertThat(PregnancyCalculator.dueDateFromLmp(d("2026-01-01"))).isEqualTo(d("2026-10-08"))
    }

    @Test fun progressAtConfirmedDueDate() {
        val due = d("2026-10-08")
        val p = PregnancyCalculator.progress(due, today = due)
        assertThat(p.weeksCompleted).isEqualTo(40)
        assertThat(p.daysRemaining).isEqualTo(0)
        assertThat(p.trimester).isEqualTo(3)
    }

    @Test fun progressMidPregnancy() {
        val due = d("2026-10-08")        // lmp 2026-01-01
        val p = PregnancyCalculator.progress(due, today = d("2026-04-23")) // 112 days after lmp = week 16
        assertThat(p.weeksCompleted).isEqualTo(16)
        assertThat(p.dayInWeek).isEqualTo(0)
        assertThat(p.trimester).isEqualTo(2)
        assertThat(p.daysRemaining).isEqualTo(168)
    }

    @Test fun overdueGivesNegativeDaysRemaining() {
        val due = d("2026-10-08")
        val p = PregnancyCalculator.progress(due, today = d("2026-10-11"))
        assertThat(p.daysRemaining).isEqualTo(-3)
        assertThat(p.weeksCompleted).isAtLeast(40)
    }
}
