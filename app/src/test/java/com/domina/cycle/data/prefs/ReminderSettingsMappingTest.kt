package com.domina.cycle.data.prefs

import com.domina.cycle.domain.reminders.ReminderSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

class ReminderSettingsMappingTest {
    @Test fun nudgeTimeRoundTripsAsMinutesOfDay() {
        val t = LocalTime.of(20, 30)
        assertThat(ReminderSettingsCodec.timeToMinutes(t)).isEqualTo(1230)
        assertThat(ReminderSettingsCodec.minutesToTime(1230)).isEqualTo(t)
    }

    @Test fun defaultsAreReasonable() {
        val s = ReminderSettings()
        assertThat(s.periodAlerts).isTrue()
        assertThat(s.dailyNudge).isFalse()
        assertThat(s.dailyNudgeTime).isEqualTo(LocalTime.of(20, 0))
        assertThat(s.periodLogReminders).isTrue()
    }

    @Test fun periodLogRemindersCanBeDisabled() {
        val s = ReminderSettings(periodLogReminders = false)
        assertThat(s.periodLogReminders).isFalse()
    }
}
