package com.domina.cycle.data.prefs

import java.time.LocalTime

object ReminderSettingsCodec {
    fun timeToMinutes(t: LocalTime): Int = t.hour * 60 + t.minute
    fun minutesToTime(m: Int): LocalTime = LocalTime.of(m / 60, m % 60)
}
