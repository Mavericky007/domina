package com.domina.cycle.domain.prediction

import java.time.LocalDate

/** A discrete menstrual period derived from logged flow days. */
data class LoggedPeriod(val start: LocalDate, val lengthDays: Int)
