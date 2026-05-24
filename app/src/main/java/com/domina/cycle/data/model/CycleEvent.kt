package com.domina.cycle.data.model

import java.time.LocalDate

/** A period boundary, used to anchor cycle predictions in later phases. */
data class CycleEvent(
    val id: Long = 0,
    val date: LocalDate,
    val type: Type,
) {
    enum class Type { PERIOD_START, PERIOD_END }
}
