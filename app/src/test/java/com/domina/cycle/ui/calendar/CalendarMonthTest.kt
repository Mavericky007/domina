package com.domina.cycle.ui.calendar

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.YearMonth

class CalendarMonthTest {
    @Test fun leadingBlanksAlignFirstDayToWeekdayMondayStart() {
        // May 2026: the 1st is a Friday -> Mon-start => 4 leading blanks
        val cells = CalendarMonth.cells(YearMonth.of(2026, 5))
        assertThat(cells.take(4)).containsExactly(null, null, null, null).inOrder()
        assertThat(cells[4]).isEqualTo(1)
        assertThat(cells.filterNotNull()).hasSize(31)
        assertThat(cells.size % 7).isEqualTo(0) // padded to full weeks
    }
}
