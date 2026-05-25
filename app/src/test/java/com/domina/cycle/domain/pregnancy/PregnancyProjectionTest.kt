package com.domina.cycle.domain.pregnancy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PregnancyProjectionTest {
    // due = 2026-12-31 -> start (LMP) = due - 280 = 2026-03-26
    private val due = LocalDate.parse("2026-12-31")

    @Test fun startMonthIsTrimester1() {
        val m = PregnancyProjection.marksFor(due, YearMonth.of(2026, 3))
        // 2026-03-26 is week 0 -> trimester 1
        assertThat(m[26]).isEqualTo(1)
        assertThat(m[25]).isNull() // before conception/LMP span
    }

    @Test fun lateMonthsAreTrimester3() {
        val m = PregnancyProjection.marksFor(due, YearMonth.of(2026, 12))
        assertThat(m[31]).isEqualTo(3) // due date
        assertThat(m[1]).isEqualTo(3)
    }

    @Test fun outsideSpanIsEmpty() {
        assertThat(PregnancyProjection.marksFor(due, YearMonth.of(2026, 1))).isEmpty()
    }
}
