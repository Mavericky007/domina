package com.domina.cycle.domain.guidance

import com.domina.cycle.domain.prediction.CyclePhase

data class PhaseGuidance(
    val phase: CyclePhase,
    val title: String,
    val moodForecast: String,
    val move: String,
    val eat: String,
    val plan: String,
)
