package com.domina.cycle.domain.checkin

import com.domina.cycle.domain.pregnancy.AppMode
import java.time.LocalDate

/** A single quick notification check-in. Score is 1..3 (meaning depends on the kind). */
enum class CheckInKind { MOOD, ENERGY, NAUSEA, FATIGUE, BLOATING }

/** One of the three tappable buttons for a check-in. */
data class CheckInOption(val emoji: String, val label: String, val score: Int)

data class CheckIn(val epochDay: Long, val kind: CheckInKind, val score: Int)

object CheckInPrompts {
    fun title(kind: CheckInKind): String = when (kind) {
        CheckInKind.MOOD -> "How are you feeling?"
        CheckInKind.ENERGY -> "How's your energy?"
        CheckInKind.NAUSEA -> "Any nausea right now?"
        CheckInKind.FATIGUE -> "How tired are you?"
        CheckInKind.BLOATING -> "Feeling bloated?"
    }

    /** Three options, ordered low→high score. For mood/energy higher = better; for symptoms higher = worse. */
    fun options(kind: CheckInKind): List<CheckInOption> = when (kind) {
        CheckInKind.MOOD -> listOf(
            CheckInOption("🙁", "Low", 1), CheckInOption("😐", "Okay", 2), CheckInOption("🙂", "Good", 3),
        )
        CheckInKind.ENERGY -> listOf(
            CheckInOption("😴", "Low", 1), CheckInOption("🙂", "Medium", 2), CheckInOption("⚡", "High", 3),
        )
        CheckInKind.NAUSEA -> listOf(
            CheckInOption("🙂", "None", 1), CheckInOption("😐", "A little", 2), CheckInOption("😖", "A lot", 3),
        )
        CheckInKind.FATIGUE -> listOf(
            CheckInOption("🙂", "Fine", 1), CheckInOption("😐", "Tired", 2), CheckInOption("😫", "Wiped", 3),
        )
        CheckInKind.BLOATING -> listOf(
            CheckInOption("🙂", "None", 1), CheckInOption("😐", "A bit", 2), CheckInOption("😣", "A lot", 3),
        )
    }
}

/** Which check-in each of the 3 daily slots asks, given the current app mode. Mood is asked daily. */
object CheckInSchedule {
    private val PREGNANCY_SYMPTOMS = listOf(CheckInKind.NAUSEA, CheckInKind.FATIGUE, CheckInKind.BLOATING)

    fun kindsFor(mode: AppMode, date: LocalDate): List<CheckInKind> = when (mode) {
        AppMode.PREGNANCY -> {
            val d = (date.toEpochDay() % PREGNANCY_SYMPTOMS.size).toInt()
            listOf(CheckInKind.MOOD, PREGNANCY_SYMPTOMS[d], PREGNANCY_SYMPTOMS[(d + 1) % PREGNANCY_SYMPTOMS.size])
        }
        else -> listOf(CheckInKind.MOOD, CheckInKind.ENERGY, CheckInKind.MOOD)
    }

    /** The kind for a specific slot (0..2) on a date+mode. */
    fun kindForSlot(mode: AppMode, date: LocalDate, slot: Int): CheckInKind =
        kindsFor(mode, date).getOrElse(slot) { CheckInKind.MOOD }

    /** Check-ins only fire during waking hours — never 9 PM–9 AM. */
    const val ACTIVE_START_HOUR = 9   // 09:00 inclusive
    const val ACTIVE_END_HOUR = 21    // 21:00 exclusive (9 PM)
    fun isQuietHour(hour: Int): Boolean = hour < ACTIVE_START_HOUR || hour >= ACTIVE_END_HOUR
}

/** Warm, encouraging replies shown after a check-in tap — meant to make her feel loved. */
object CheckInMessages {
    fun confirmation(kind: CheckInKind, score: Int): String = when (kind) {
        CheckInKind.MOOD -> when (score) {
            3 -> "Love that 💛 So happy you're feeling good — you deserve every bit of it."
            2 -> "Thanks for checking in 💛 Hope your day keeps getting brighter."
            else -> "Sending you the biggest hug 🤍 Be gentle with yourself today — this feeling will pass."
        }
        CheckInKind.ENERGY -> when (score) {
            3 -> "Look at you shining ⚡ Go enjoy that energy!"
            2 -> "Steady and strong 💛 You're doing great."
            else -> "Rest is productive too 😌 Take it slow — you've earned it."
        }
        CheckInKind.NAUSEA -> when (score) {
            1 -> "Yay, feeling settled 💛 Keep it up!"
            2 -> "Hang in there 💛 Small sips and slow breaths help."
            else -> "I'm sorry it's rough 🤍 Rest up — you're growing something amazing."
        }
        CheckInKind.FATIGUE -> when (score) {
            1 -> "Lovely — well-rested and glowing ✨"
            2 -> "Take little breaks today 💛 You're doing so much."
            else -> "You're carrying a lot 🤍 Please rest — you deserve it."
        }
        CheckInKind.BLOATING -> when (score) {
            1 -> "Comfy and good 💛 Nice!"
            2 -> "Comfy clothes and water help 💛 Be kind to yourself."
            else -> "Sorry you're uncomfortable 🤍 Rest and warmth — it eases soon."
        }
    }

    fun confirmationTitle(): String = "Got it 💛"
}

object CheckInStats {
    /** Rounded average score (1..3) for a kind on a given day, or null if no check-ins. */
    fun dailyScore(entries: List<CheckIn>, kind: CheckInKind, epochDay: Long): Int? {
        val todays = entries.filter { it.epochDay == epochDay && it.kind == kind }
        if (todays.isEmpty()) return null
        return Math.round(todays.map { it.score }.average()).toInt()
    }

    /** The day's overall mood emoji from averaged MOOD check-ins, or null. */
    fun dailyMoodEmoji(entries: List<CheckIn>, epochDay: Long): String? =
        dailyScore(entries, CheckInKind.MOOD, epochDay)?.let { score ->
            CheckInPrompts.options(CheckInKind.MOOD).firstOrNull { it.score == score }?.emoji
        }
}
