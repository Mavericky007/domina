package com.domina.cycle.domain.reminders

import com.domina.cycle.domain.prediction.CyclePrediction
import java.time.LocalDateTime

/** Pure timing logic: which reminders should fire, and when, given a prediction & settings. */
object ReminderScheduler {
    fun compute(
        prediction: CyclePrediction,
        settings: ReminderSettings,
        now: LocalDateTime,
    ): List<ScheduledReminder> {
        val out = mutableListOf<ScheduledReminder>()
        val morning = settings.morningHour

        if (settings.periodAlerts) {
            prediction.nextPeriodDate?.let { next ->
                out += ScheduledReminder(
                    ReminderType.PERIOD_SOON,
                    next.minusDays(settings.periodLeadDays.toLong()).atTime(morning, 0),
                    "Period coming up 🩸",
                    "Your period may start in about ${settings.periodLeadDays} days. A good time to stock up 💛",
                )
                out += ScheduledReminder(
                    ReminderType.PERIOD_TODAY, next.atTime(morning, 0),
                    "Period may start today 🩸", "Today's the day your period is predicted. Take it easy 💛",
                )
                out += ScheduledReminder(
                    ReminderType.PERIOD_LATE, next.plusDays(2).atTime(morning, 0),
                    "Period is a couple days late", "No period logged yet — tap to log, or just keep an eye out.",
                )
            }
        }
        if (settings.fertilityAlerts) {
            prediction.fertileWindowStart?.let {
                out += ScheduledReminder(
                    ReminderType.FERTILE_WINDOW_OPEN, it.atTime(morning, 0),
                    "Fertile window opening 🌸", "Your most fertile days are starting.",
                )
            }
            prediction.ovulationDate?.let {
                out += ScheduledReminder(
                    ReminderType.OVULATION_DAY, it.atTime(morning, 0),
                    "Ovulation day 🌸", "Estimated ovulation is today — peak fertility.",
                )
            }
        }
        if (settings.dailyNudge) {
            val todayAt = now.toLocalDate().atTime(settings.dailyNudgeTime)
            val fireAt = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
            out += ScheduledReminder(
                ReminderType.DAILY_LOG_NUDGE, fireAt,
                "How are you feeling today? 💛", "A quick tap to log your mood, flow, or symptoms.",
            )
        }
        return out.filter { it.fireAt.isAfter(now) }
    }
}
