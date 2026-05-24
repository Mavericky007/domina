package com.domina.cycle.domain.guidance

import com.domina.cycle.domain.prediction.CyclePhase

/** Bundled, offline per-phase guidance. Gentle and encouraging — not medical advice. */
object PhaseGuide {
    fun forPhase(phase: CyclePhase): PhaseGuidance = when (phase) {
        CyclePhase.MENSTRUAL -> PhaseGuidance(
            phase = phase,
            title = "🩸 Menstrual phase — rest & restore",
            moodForecast = "Energy is at its lowest and feel-good hormones dip, so you might feel tired, tender, or a little foggy. Be gentle with yourself 💛",
            move = "Rest and restore: gentle yoga, stretching, easy walks.",
            eat = "Iron-rich foods (spinach, lentils, red meat) with vitamin C to absorb it; omega-3s; warm, comforting meals.",
            plan = "Keep the load light. Journaling, cozy nights, and early sleep are perfect now.",
        )
        CyclePhase.FOLLICULAR -> PhaseGuidance(
            phase = phase,
            title = "🌱 Follicular phase — rising energy",
            moodForecast = "Estrogen is climbing, so you'll likely feel more upbeat, motivated, and clear-headed. A great stretch to start something new.",
            move = "Ramp it up: cardio, hikes, a brisk walk, or try a new class.",
            eat = "Lean proteins and complex carbs (quinoa, brown rice), avocado, seeds, leafy greens, fermented foods.",
            plan = "Brain's sharp — schedule big projects, tough conversations, and creative work.",
        )
        CyclePhase.OVULATION -> PhaseGuidance(
            phase = phase,
            title = "🌸 Ovulation — peak energy",
            moodForecast = "Energy, mood, and confidence often peak now, and you may feel your most social.",
            move = "Go for it: HIIT, spin, kickboxing — your strength and stamina are high.",
            eat = "Berries and cruciferous veg (broccoli, brussels sprouts); light, fresh foods.",
            plan = "Best window for presentations, dates, social plans, and big asks.",
        )
        CyclePhase.LUTEAL -> PhaseGuidance(
            phase = phase,
            title = "🌙 Luteal phase — winding down",
            moodForecast = "Energy gradually dips and PMS, cravings, or irritability may show up later in the phase. Extra kindness helps 💛",
            move = "Taper off: strength early on, then walks, pilates, tai chi as energy fades.",
            eat = "Fiber and magnesium (pumpkin seeds, dark chocolate); stay hydrated; ease up on sugar, salt, and caffeine.",
            plan = "Wrap things up and tidy loose ends. Lean into self-care and cozy, smaller gatherings.",
        )
    }
}
