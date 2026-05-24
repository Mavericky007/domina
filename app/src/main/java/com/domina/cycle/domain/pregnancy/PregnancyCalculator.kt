package com.domina.cycle.domain.pregnancy

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object PregnancyCalculator {
    private const val TERM_DAYS = 280L // 40 weeks

    fun dueDateFromLmp(lmp: LocalDate): LocalDate = lmp.plusDays(TERM_DAYS)

    fun progress(dueDate: LocalDate, today: LocalDate): PregnancyProgress {
        val daysRemaining = ChronoUnit.DAYS.between(today, dueDate).toInt()
        val completedDays = (TERM_DAYS - daysRemaining).toInt().coerceAtLeast(0)
        val weeksCompleted = completedDays / 7
        val dayInWeek = completedDays % 7
        val trimester = when {
            weeksCompleted <= 13 -> 1
            weeksCompleted <= 27 -> 2
            else -> 3
        }
        return PregnancyProgress(dueDate, weeksCompleted, dayInWeek, daysRemaining, trimester)
    }
}
