package com.domina.cycle.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DayLogTest {
    @Test fun emptyLogHasNoData() {
        val log = DayLog(date = LocalDate.of(2026, 5, 24))
        assertThat(log.isEmpty()).isTrue()
    }

    @Test fun logWithMoodIsNotEmpty() {
        val log = DayLog(date = LocalDate.of(2026, 5, 24), mood = Mood.HAPPY)
        assertThat(log.isEmpty()).isFalse()
    }
}
