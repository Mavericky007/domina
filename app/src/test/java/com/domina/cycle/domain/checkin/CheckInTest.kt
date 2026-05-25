package com.domina.cycle.domain.checkin

import com.domina.cycle.domain.pregnancy.AppMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class CheckInTest {
    private val day = LocalDate.parse("2026-05-25")
    private val epoch = day.toEpochDay()

    @Test fun cycleModeAsksMoodEnergyMood() {
        assertThat(CheckInSchedule.kindsFor(AppMode.CYCLE, day))
            .containsExactly(CheckInKind.MOOD, CheckInKind.ENERGY, CheckInKind.MOOD).inOrder()
    }

    @Test fun pregnancyModeAsksMoodPlusTwoSymptoms() {
        val kinds = CheckInSchedule.kindsFor(AppMode.PREGNANCY, day)
        assertThat(kinds[0]).isEqualTo(CheckInKind.MOOD)
        assertThat(kinds.drop(1)).containsNoneOf(CheckInKind.MOOD, CheckInKind.ENERGY)
        assertThat(kinds).hasSize(3)
    }

    @Test fun everyKindHasThreeOptions() {
        CheckInKind.entries.forEach { assertThat(CheckInPrompts.options(it)).hasSize(3) }
    }

    @Test fun dailyMoodAveragesCheckins() {
        val entries = listOf(
            CheckIn(epoch, CheckInKind.MOOD, 3),
            CheckIn(epoch, CheckInKind.MOOD, 3),
            CheckIn(epoch, CheckInKind.MOOD, 2),
            CheckIn(epoch, CheckInKind.ENERGY, 1),
        )
        assertThat(CheckInStats.dailyScore(entries, CheckInKind.MOOD, epoch)).isEqualTo(3) // avg 2.67 → 3
        assertThat(CheckInStats.dailyMoodEmoji(entries, epoch)).isEqualTo("🙂")
    }

    @Test fun noCheckinsIsNull() {
        assertThat(CheckInStats.dailyScore(emptyList(), CheckInKind.MOOD, epoch)).isNull()
        assertThat(CheckInStats.dailyMoodEmoji(emptyList(), epoch)).isNull()
    }
}
