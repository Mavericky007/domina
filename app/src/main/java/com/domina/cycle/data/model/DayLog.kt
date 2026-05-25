package com.domina.cycle.data.model

import java.time.LocalDate

data class DayLog(
    val date: LocalDate,
    val mood: Mood? = null,
    val energy: Energy? = null,
    val flow: FlowIntensity = FlowIntensity.NONE,
    val symptoms: List<String> = emptyList(),
    val bbt: Double? = null,            // basal body temperature, °C
    val cervicalMucus: CervicalMucus? = null,
    val lhResult: LhResult = LhResult.NOT_TESTED,
    val libido: Int? = null,            // 0..3
    val sleepHours: Double? = null,
    val weight: Double? = null,
    val intimacy: Intimacy = Intimacy.NONE,
    val emergencyContraception: Boolean = false,   // morning-after pill taken
    val note: String = "",
) {
    fun isEmpty(): Boolean =
        mood == null && energy == null && flow == FlowIntensity.NONE && symptoms.isEmpty() &&
            bbt == null && cervicalMucus == null && lhResult == LhResult.NOT_TESTED &&
            libido == null && sleepHours == null && weight == null &&
            intimacy == Intimacy.NONE && !emergencyContraception && note.isBlank()
}
