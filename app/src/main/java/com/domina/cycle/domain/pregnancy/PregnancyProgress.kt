package com.domina.cycle.domain.pregnancy

import java.time.LocalDate

data class PregnancyProgress(
    val dueDate: LocalDate,
    val weeksCompleted: Int,
    val dayInWeek: Int,
    val daysRemaining: Int,
    val trimester: Int,
)
