package com.domina.cycle.domain.prediction

import java.time.LocalDate

data class CyclePrediction(
    val cycleDay: Int?,
    val phase: CyclePhase?,
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    val nextPeriodDate: LocalDate?,
    val ovulationDate: LocalDate?,
    val fertileWindowStart: LocalDate?,
    val fertileWindowEnd: LocalDate?,
    val confidence: Confidence,
)
