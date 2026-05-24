# Phase 2: Prediction Engine, Phase Guidance & Cycle-Ring Dashboard — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn logged data into accurate, adaptive predictions (current cycle day & phase, next period, fertile window, ovulation, confidence) and surface them on a warm Today dashboard featuring an original circular "cycle-ring" plus per-phase mood/Move/Eat/Plan guidance.

**Architecture:** A new **pure-Kotlin `domain/` layer** (no Android deps, exhaustively unit-tested) holds the prediction engine and phase-guidance content. Cycle history is **derived from existing flow logs** (runs of consecutive flow days = one period), so no new logging UI is needed. The Today dashboard's ViewModel reactively recomputes predictions from the `DayLogRepository` Flow; a Compose `Canvas` renders the cycle ring.

**Tech Stack:** Kotlin, java.time, Jetpack Compose (Canvas/drawArc), Hilt, Coroutines/Flow. Builds on Phase 1 (Room+SQLCipher, repositories, themes, lock — all on `main`).

> Run gradle from repo root `/Users/jounaid/domina` (or `-p` it). Domain unit tests are pure JVM: `./gradlew :app:testDebugUnitTest --tests "..."` — **no device needed** for Tasks 1–4. Task 5 (UI) needs the connected device for install/launch verification.

---

## Existing code this builds on (Phase 1, already on `main`)

- `data/model/DayLog.kt` — `DayLog(date: LocalDate, mood, energy, flow: FlowIntensity, symptoms, bbt, …)`; `FlowIntensity { NONE, SPOTTING, LIGHT, MEDIUM, HEAVY }`.
- `data/repository/DayLogRepository.kt` — `fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayLog>>`, `suspend fun getByDate(date)`, `suspend fun save(log)`.
- `ui/today/TodayViewModel.kt` + `TodayScreen.kt` (basic versions — replaced here).
- `ui/nav/AppNav.kt` calls `TodayScreen(onLogToday = { nav.navigate(Destinations.LOG) })` — keep this signature.

## New file structure (Phase 2)

```
app/src/main/java/com/domina/cycle/
  domain/
    prediction/
      LoggedPeriod.kt        # data class: a derived period (start + length)
      CyclePhase.kt          # enum MENSTRUAL/FOLLICULAR/OVULATION/LUTEAL
      Confidence.kt          # enum NONE/LOW/MEDIUM/HIGH
      CyclePrediction.kt     # data class: the full prediction result
      PeriodDeriver.kt       # pure: List<LocalDate> flow days -> List<LoggedPeriod>
      CyclePredictor.kt      # pure: List<LoggedPeriod> + today -> CyclePrediction
    guidance/
      PhaseGuidance.kt       # data class: per-phase copy
      PhaseGuide.kt          # the 4-phase content table + forPhase()
  ui/today/
    TodayViewModel.kt        # MODIFY: reactive prediction + guidance + today log
    CycleRing.kt             # NEW: Compose Canvas ring
    TodayScreen.kt           # MODIFY: ring + chips + guidance card
app/src/test/java/com/domina/cycle/
  domain/prediction/PeriodDeriverTest.kt, CyclePredictorTest.kt
  domain/guidance/PhaseGuideTest.kt
  ui/today/TodayUiStateTest.kt
```

---

## Task 1: PeriodDeriver (group flow days into periods)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/prediction/LoggedPeriod.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/prediction/PeriodDeriver.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/prediction/PeriodDeriverTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.prediction

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PeriodDeriverTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun emptyInputGivesNoPeriods() {
        assertThat(PeriodDeriver.derive(emptyList())).isEmpty()
    }

    @Test fun singleFlowDayIsOnePeriodOfLengthOne() {
        val r = PeriodDeriver.derive(listOf(d("2026-05-01")))
        assertThat(r).containsExactly(LoggedPeriod(d("2026-05-01"), 1))
    }

    @Test fun consecutiveDaysCollapseIntoOnePeriod() {
        val r = PeriodDeriver.derive(listOf(d("2026-05-01"), d("2026-05-02"), d("2026-05-03")))
        assertThat(r).containsExactly(LoggedPeriod(d("2026-05-01"), 3))
    }

    @Test fun gapStartsANewPeriodAndInputIsSortedAndDeduped() {
        val r = PeriodDeriver.derive(
            listOf(d("2026-05-02"), d("2026-05-01"), d("2026-05-01"), d("2026-05-29"), d("2026-05-30"))
        )
        assertThat(r).containsExactly(
            LoggedPeriod(d("2026-05-01"), 2),
            LoggedPeriod(d("2026-05-29"), 2),
        ).inOrder()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PeriodDeriverTest*"`
Expected: FAIL — unresolved `PeriodDeriver` / `LoggedPeriod`.

- [ ] **Step 3: Implement `LoggedPeriod` and `PeriodDeriver`**

`LoggedPeriod.kt`:
```kotlin
package com.domina.cycle.domain.prediction

import java.time.LocalDate

/** A discrete menstrual period derived from logged flow days. */
data class LoggedPeriod(val start: LocalDate, val lengthDays: Int)
```
`PeriodDeriver.kt`:
```kotlin
package com.domina.cycle.domain.prediction

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Groups runs of consecutive flow days into discrete [LoggedPeriod]s. */
object PeriodDeriver {
    fun derive(flowDates: List<LocalDate>): List<LoggedPeriod> {
        if (flowDates.isEmpty()) return emptyList()
        val sorted = flowDates.distinct().sorted()
        val periods = mutableListOf<LoggedPeriod>()
        var start = sorted.first()
        var prev = sorted.first()
        var length = 1
        for (date in sorted.drop(1)) {
            if (ChronoUnit.DAYS.between(prev, date) == 1L) {
                length++
            } else {
                periods.add(LoggedPeriod(start, length))
                start = date
                length = 1
            }
            prev = date
        }
        periods.add(LoggedPeriod(start, length))
        return periods
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PeriodDeriverTest*"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/prediction/LoggedPeriod.kt \
        app/src/main/java/com/domina/cycle/domain/prediction/PeriodDeriver.kt \
        app/src/test/java/com/domina/cycle/domain/prediction/PeriodDeriverTest.kt
git commit -m "feat: derive discrete periods from consecutive flow days"
```

---

## Task 2: CyclePredictor (adaptive predictions + confidence)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/prediction/CyclePhase.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/prediction/Confidence.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/prediction/CyclePrediction.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/prediction/CyclePredictor.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/prediction/CyclePredictorTest.kt`

Behavior contract (all deterministic):
- No periods → `confidence = NONE`, day/phase/dates null, `averageCycleLength = 28`, `averagePeriodLength = 5`.
- `averageCycleLength` = rounded mean of the gaps between the last ≤6 period starts (default 28 if <2 periods).
- `averagePeriodLength` = rounded mean of the last ≤6 period lengths (default 5).
- `cycleDay` = days from last period start to today + 1 (null if today is before the last start).
- `nextPeriodDate` = lastStart + averageCycleLength. `ovulationDate` = nextPeriodDate − 14 (fixed luteal). `fertileWindow` = [ovulation−5, ovulation+1].
- Phase by day-of-cycle: MENSTRUAL `1..periodLen`; OVULATION within ±1 of ovulation day (= cycleLen−14); FOLLICULAR between menstrual and ovulation; otherwise LUTEAL (includes "late").
- Confidence: 1 period → LOW; gaps range (max−min over last ≤6) ≤4 with ≥3 gaps → HIGH; ≤9 → MEDIUM; else LOW.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.prediction

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class CyclePredictorTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun starts(vararg s: String, len: Int = 5) = s.map { LoggedPeriod(d(it), len) }

    @Test fun noDataYieldsNoneConfidenceAndDefaults() {
        val p = CyclePredictor.predict(emptyList(), d("2026-05-10"))
        assertThat(p.confidence).isEqualTo(Confidence.NONE)
        assertThat(p.cycleDay).isNull()
        assertThat(p.phase).isNull()
        assertThat(p.averageCycleLength).isEqualTo(28)
        assertThat(p.nextPeriodDate).isNull()
    }

    @Test fun singlePeriodUsesDefaultsAndLowConfidence() {
        val p = CyclePredictor.predict(starts("2026-05-01"), d("2026-05-03"))
        assertThat(p.confidence).isEqualTo(Confidence.LOW)
        assertThat(p.cycleDay).isEqualTo(3)              // May 1 -> May 3 = day 3
        assertThat(p.averageCycleLength).isEqualTo(28)
        assertThat(p.nextPeriodDate).isEqualTo(d("2026-05-29"))
        assertThat(p.phase).isEqualTo(CyclePhase.MENSTRUAL) // day 3 <= 5
    }

    @Test fun regularCyclesGiveHighConfidenceAndCorrectAverages() {
        // four starts exactly 28 apart
        val periods = starts("2026-02-01", "2026-03-01", "2026-03-29", "2026-04-26")
        val p = CyclePredictor.predict(periods, d("2026-05-05")) // day 10 of current cycle
        assertThat(p.averageCycleLength).isEqualTo(28)
        assertThat(p.confidence).isEqualTo(Confidence.HIGH)
        assertThat(p.cycleDay).isEqualTo(10)
        assertThat(p.phase).isEqualTo(CyclePhase.FOLLICULAR) // 6..12
        assertThat(p.nextPeriodDate).isEqualTo(d("2026-05-24"))
        assertThat(p.ovulationDate).isEqualTo(d("2026-05-10")) // next - 14
        assertThat(p.fertileWindowStart).isEqualTo(d("2026-05-05"))
        assertThat(p.fertileWindowEnd).isEqualTo(d("2026-05-11"))
    }

    @Test fun phaseBoundariesForA28DayCycle() {
        val periods = starts("2026-02-01", "2026-03-01", "2026-03-29", "2026-04-26")
        fun phaseOnDay(day: Int) =
            CyclePredictor.predict(periods, d("2026-04-26").plusDays((day - 1).toLong())).phase
        assertThat(phaseOnDay(3)).isEqualTo(CyclePhase.MENSTRUAL)   // 1..5
        assertThat(phaseOnDay(9)).isEqualTo(CyclePhase.FOLLICULAR)  // 6..12
        assertThat(phaseOnDay(14)).isEqualTo(CyclePhase.OVULATION)  // 13..15
        assertThat(phaseOnDay(20)).isEqualTo(CyclePhase.LUTEAL)     // 16..28
    }

    @Test fun irregularCyclesGiveMediumOrLowNotHigh() {
        val periods = starts("2026-01-01", "2026-01-26", "2026-03-02", "2026-03-28") // gaps 25,35,26 -> range 10
        val p = CyclePredictor.predict(periods, d("2026-04-02"))
        assertThat(p.confidence).isEqualTo(Confidence.LOW) // range 10 > 9
    }

    @Test fun latePeriodIsLutealWithDayPastCycleLength() {
        val periods = starts("2026-02-01", "2026-03-01", "2026-03-29", "2026-04-26")
        val p = CyclePredictor.predict(periods, d("2026-05-30")) // day 35, past 28
        assertThat(p.cycleDay).isEqualTo(35)
        assertThat(p.phase).isEqualTo(CyclePhase.LUTEAL)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*CyclePredictorTest*"`
Expected: FAIL — unresolved `CyclePredictor` / `CyclePhase` / `Confidence` / `CyclePrediction`.

- [ ] **Step 3: Implement the enums, result type, and predictor**

`CyclePhase.kt`:
```kotlin
package com.domina.cycle.domain.prediction

enum class CyclePhase { MENSTRUAL, FOLLICULAR, OVULATION, LUTEAL }
```
`Confidence.kt`:
```kotlin
package com.domina.cycle.domain.prediction

enum class Confidence { NONE, LOW, MEDIUM, HIGH }
```
`CyclePrediction.kt`:
```kotlin
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
```
`CyclePredictor.kt`:
```kotlin
package com.domina.cycle.domain.prediction

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Adaptive cycle predictions derived from logged period history. Pure & deterministic. */
object CyclePredictor {
    const val DEFAULT_CYCLE_LENGTH = 28
    const val DEFAULT_PERIOD_LENGTH = 5
    private const val LUTEAL_LENGTH = 14
    private const val RECENT_WINDOW = 6

    fun predict(periods: List<LoggedPeriod>, today: LocalDate): CyclePrediction {
        if (periods.isEmpty()) {
            return CyclePrediction(
                cycleDay = null, phase = null,
                averageCycleLength = DEFAULT_CYCLE_LENGTH,
                averagePeriodLength = DEFAULT_PERIOD_LENGTH,
                nextPeriodDate = null, ovulationDate = null,
                fertileWindowStart = null, fertileWindowEnd = null,
                confidence = Confidence.NONE,
            )
        }
        val sorted = periods.sortedBy { it.start }
        val lastStart = sorted.last().start

        val gaps = sorted.map { it.start }
            .zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        val recentGaps = gaps.takeLast(RECENT_WINDOW)
        val avgCycle = if (recentGaps.isEmpty()) DEFAULT_CYCLE_LENGTH else recentGaps.average().roundToInt()
        val recentLengths = sorted.takeLast(RECENT_WINDOW).map { it.lengthDays }
        val avgPeriod = if (recentLengths.isEmpty()) DEFAULT_PERIOD_LENGTH else recentLengths.average().roundToInt()

        val cycleDay = if (!today.isBefore(lastStart))
            ChronoUnit.DAYS.between(lastStart, today).toInt() + 1 else null

        val nextPeriod = lastStart.plusDays(avgCycle.toLong())
        val ovulation = nextPeriod.minusDays(LUTEAL_LENGTH.toLong())
        val ovulationDay = avgCycle - LUTEAL_LENGTH

        val phase = cycleDay?.let { phaseFor(it, avgPeriod, ovulationDay) }

        return CyclePrediction(
            cycleDay = cycleDay,
            phase = phase,
            averageCycleLength = avgCycle,
            averagePeriodLength = avgPeriod,
            nextPeriodDate = nextPeriod,
            ovulationDate = ovulation,
            fertileWindowStart = ovulation.minusDays(5),
            fertileWindowEnd = ovulation.plusDays(1),
            confidence = confidenceFor(gaps),
        )
    }

    private fun phaseFor(day: Int, periodLen: Int, ovulationDay: Int): CyclePhase = when {
        day <= periodLen -> CyclePhase.MENSTRUAL
        day in (ovulationDay - 1)..(ovulationDay + 1) -> CyclePhase.OVULATION
        day < ovulationDay - 1 -> CyclePhase.FOLLICULAR
        else -> CyclePhase.LUTEAL
    }

    private fun confidenceFor(gaps: List<Int>): Confidence {
        if (gaps.isEmpty()) return Confidence.LOW
        val recent = gaps.takeLast(RECENT_WINDOW)
        val range = recent.max() - recent.min()
        return when {
            recent.size >= 3 && range <= 4 -> Confidence.HIGH
            range <= 9 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*CyclePredictorTest*"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/prediction/CyclePhase.kt \
        app/src/main/java/com/domina/cycle/domain/prediction/Confidence.kt \
        app/src/main/java/com/domina/cycle/domain/prediction/CyclePrediction.kt \
        app/src/main/java/com/domina/cycle/domain/prediction/CyclePredictor.kt \
        app/src/test/java/com/domina/cycle/domain/prediction/CyclePredictorTest.kt
git commit -m "feat: adaptive cycle predictor (phase, next period, fertile window, confidence)"
```

---

## Task 3: Phase guidance content

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/guidance/PhaseGuidance.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/guidance/PhaseGuide.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/guidance/PhaseGuideTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.guidance

import com.domina.cycle.domain.prediction.CyclePhase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PhaseGuideTest {
    @Test fun everyPhaseHasCompleteNonBlankGuidance() {
        CyclePhase.entries.forEach { phase ->
            val g = PhaseGuide.forPhase(phase)
            assertThat(g.phase).isEqualTo(phase)
            assertThat(g.title).isNotEmpty()
            assertThat(g.moodForecast).isNotEmpty()
            assertThat(g.move).isNotEmpty()
            assertThat(g.eat).isNotEmpty()
            assertThat(g.plan).isNotEmpty()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PhaseGuideTest*"`
Expected: FAIL — unresolved `PhaseGuide` / `PhaseGuidance`.

- [ ] **Step 3: Implement `PhaseGuidance` and `PhaseGuide`**

`PhaseGuidance.kt`:
```kotlin
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
```
`PhaseGuide.kt` (warm, gentle, non-medical tone — content from the design spec):
```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PhaseGuideTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/guidance/PhaseGuidance.kt \
        app/src/main/java/com/domina/cycle/domain/guidance/PhaseGuide.kt \
        app/src/test/java/com/domina/cycle/domain/guidance/PhaseGuideTest.kt
git commit -m "feat: bundled per-phase mood/Move/Eat/Plan guidance"
```

---

## Task 4: Today dashboard state (reactive prediction + guidance)

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/today/TodayViewModel.kt` (replace the Phase-1 version)
- Test: `app/src/test/java/com/domina/cycle/ui/today/TodayUiStateTest.kt`

- [ ] **Step 1: Write the failing test (pure `buildState` function)**

```kotlin
package com.domina.cycle.ui.today

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.prediction.CyclePhase
import com.domina.cycle.domain.prediction.Confidence
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class TodayUiStateTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun flowLog(date: String) = DayLog(date = d(date), flow = FlowIntensity.MEDIUM)

    @Test fun noFlowLogsYieldNoneConfidenceAndNoGuidance() {
        val state = TodayViewModel.buildState(emptyList(), d("2026-05-10"))
        assertThat(state.prediction.confidence).isEqualTo(Confidence.NONE)
        assertThat(state.guidance).isNull()
    }

    @Test fun derivesPredictionAndGuidanceFromFlowLogs() {
        // period starts on the 1st of four consecutive months (~monthly), today = day ~10
        val logs = listOf(
            flowLog("2026-02-01"), flowLog("2026-03-01"),
            flowLog("2026-03-29"), flowLog("2026-04-26"),
        )
        val state = TodayViewModel.buildState(logs, d("2026-05-05"))
        assertThat(state.prediction.cycleDay).isEqualTo(10)
        assertThat(state.prediction.phase).isEqualTo(CyclePhase.FOLLICULAR)
        assertThat(state.guidance).isNotNull()
        assertThat(state.guidance!!.phase).isEqualTo(CyclePhase.FOLLICULAR)
    }

    @Test fun exposesTodaysLogWhenPresent() {
        val logs = listOf(flowLog("2026-05-05").copy(date = d("2026-05-10")))
        val state = TodayViewModel.buildState(logs, d("2026-05-10"))
        assertThat(state.todayLog?.date).isEqualTo(d("2026-05-10"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*TodayUiStateTest*"`
Expected: FAIL — `TodayViewModel.buildState` / `TodayUiState` not found (current TodayViewModel has no such API).

- [ ] **Step 3: Replace `TodayViewModel.kt`**

```kotlin
package com.domina.cycle.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.domain.guidance.PhaseGuidance
import com.domina.cycle.domain.guidance.PhaseGuide
import com.domina.cycle.domain.prediction.CyclePrediction
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class TodayUiState(
    val today: LocalDate,
    val prediction: CyclePrediction,
    val guidance: PhaseGuidance?,
    val todayLog: DayLog?,
) {
    companion object {
        fun empty(today: LocalDate) = TodayUiState(
            today = today,
            prediction = CyclePredictor.predict(emptyList(), today),
            guidance = null,
            todayLog = null,
        )
    }
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    repository: DayLogRepository,
) : ViewModel() {
    val today: LocalDate = LocalDate.now()

    val state: StateFlow<TodayUiState> =
        repository.observeRange(today.minusDays(LOOKBACK_DAYS), today)
            .map { logs -> buildState(logs, today) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.empty(today))

    companion object {
        private const val LOOKBACK_DAYS = 400L

        /** Pure transform from logs to dashboard state — unit-tested directly. */
        fun buildState(logs: List<DayLog>, today: LocalDate): TodayUiState {
            val flowDates = logs.filter { it.flow != FlowIntensity.NONE }.map { it.date }
            val periods = PeriodDeriver.derive(flowDates)
            val prediction = CyclePredictor.predict(periods, today)
            val guidance = prediction.phase?.let { PhaseGuide.forPhase(it) }
            val todayLog = logs.firstOrNull { it.date == today }
            return TodayUiState(today, prediction, guidance, todayLog)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*TodayUiStateTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Run the full unit suite to confirm no regression**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (Phase 1 tests + all new Phase 2 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/domina/cycle/ui/today/TodayViewModel.kt \
        app/src/test/java/com/domina/cycle/ui/today/TodayUiStateTest.kt
git commit -m "feat: Today dashboard state derives predictions & guidance from logs"
```

---

## Task 5: Cycle-ring composable + Today dashboard UI

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/today/CycleRing.kt`
- Modify: `app/src/main/java/com/domina/cycle/ui/today/TodayScreen.kt` (replace the Phase-1 version)

This task is UI. The logic it depends on is already fully unit-tested (Tasks 1–4). Verify by: it compiles, the app installs and launches without crashing, and a Compose `@Preview` renders the ring. Full visual tap-through is left to the user.

- [ ] **Step 1: Implement `CycleRing.kt`**

```kotlin
package com.domina.cycle.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.domina.cycle.domain.prediction.CyclePhase

private val MenstrualColor = Color(0xFFFF7A8A)
private val FollicularColor = Color(0xFF1FB6A6)
private val OvulationColor = Color(0xFFFFC857)
private val LutealColor = Color(0xFF7C5CBF)

/**
 * Circular cycle ring: four phase arcs sized by their day-spans, with a marker at the
 * current cycle day and the day/phase in the center.
 */
@Composable
fun CycleRing(
    cycleDay: Int?,
    cycleLength: Int,
    periodLength: Int,
    phase: CyclePhase?,
    phaseLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val stroke = 26.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            val ovulationDay = (cycleLength - 14).coerceAtLeast(periodLength + 1)

            // day-of-cycle spans for each phase (1-based, inclusive)
            val spans = listOf(
                MenstrualColor to (1..periodLength),
                FollicularColor to ((periodLength + 1) until (ovulationDay - 1)),
                OvulationColor to ((ovulationDay - 1)..(ovulationDay + 1)),
                LutealColor to ((ovulationDay + 2)..cycleLength),
            )
            val degPerDay = 360f / cycleLength
            spans.forEach { (color, range) ->
                if (!range.isEmpty()) {
                    val startAngle = -90f + (range.first - 1) * degPerDay
                    val sweep = (range.last - range.first + 1) * degPerDay
                    drawArc(
                        color = color, startAngle = startAngle, sweepAngle = sweep,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                }
            }
            // current-day marker
            if (cycleDay != null) {
                val clamped = cycleDay.coerceIn(1, cycleLength)
                val angle = Math.toRadians((-90f + (clamped - 0.5f) * degPerDay).toDouble())
                val r = (arcSize.width / 2)
                val cx = size.width / 2 + r * kotlin.math.cos(angle).toFloat()
                val cy = size.height / 2 + r * kotlin.math.sin(angle).toFloat()
                drawCircle(color = Color.White, radius = stroke * 0.55f, center = Offset(cx, cy))
                drawCircle(color = Color(0xFF333333), radius = stroke * 0.30f, center = Offset(cx, cy))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (cycleDay != null) "CYCLE DAY" else "WELCOME", style = MaterialTheme.typography.labelSmall)
            Text(
                text = cycleDay?.toString() ?: "—",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(phaseLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}
```

- [ ] **Step 2: Replace `TodayScreen.kt`**

```kotlin
package com.domina.cycle.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.domina.cycle.domain.guidance.PhaseGuidance
import com.domina.cycle.domain.prediction.Confidence
import com.domina.cycle.domain.prediction.CyclePrediction
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun TodayScreen(onLogToday: () -> Unit, vm: TodayViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val p = state.prediction

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Hello 💛", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        CycleRing(
            cycleDay = p.cycleDay,
            cycleLength = p.averageCycleLength,
            periodLength = p.averagePeriodLength,
            phase = p.phase,
            phaseLabel = state.guidance?.title?.substringAfter(' ')?.substringBefore(" —") ?: "Let's begin",
        )

        Spacer(Modifier.height(12.dp))

        if (p.confidence == Confidence.NONE) {
            Text(
                "Log a few periods and I'll start predicting your cycle & phases 💛",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.nextPeriodDate?.let { ChipInfo("🩸 Period", daysLabel(state.today, it)) }
                p.fertileWindowStart?.let { ChipInfo("🌸 Fertile", daysLabel(state.today, it)) }
            }
            if (p.confidence == Confidence.LOW) {
                Spacer(Modifier.height(6.dp))
                Text("Still learning your rhythm — predictions get sharper as you log.",
                    style = MaterialTheme.typography.labelMedium)
            }
        }

        state.guidance?.let { g ->
            Spacer(Modifier.height(16.dp))
            GuidanceCard(g)
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = onLogToday) { Text(if (state.todayLog == null) "Log today" else "Edit today's log") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChipInfo(label: String, value: String) {
    AssistChip(onClick = {}, label = { Text("$label · $value") })
}

@Composable
private fun GuidanceCard(g: PhaseGuidance) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(g.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(g.moodForecast, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            GuidanceRow("🏃‍♀️ Move", g.move)
            GuidanceRow("🥗 Eat", g.eat)
            GuidanceRow("✨ Plan", g.plan)
            Spacer(Modifier.height(8.dp))
            Text("Gentle guidance, not medical advice 💛", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GuidanceRow(label: String, body: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}

private fun daysLabel(today: LocalDate, target: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return when {
        days < 0 -> "${-days}d ago"
        days == 0 -> "today"
        days == 1 -> "tomorrow"
        else -> "in ${days}d"
    }
}
```

> Note: `collectAsStateWithLifecycle` comes from `androidx.lifecycle:lifecycle-runtime-compose` (already a dependency from Phase 1). `AssistChip`, `Card` are Material3 (already used).

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Install and verify launch on the connected device**

Run: `./gradlew :app:installDebug`
Then: `adb shell am start -n com.domina.cycle/.MainActivity`, wait ~3s, and check `adb logcat -d` for any `FATAL EXCEPTION`/`AndroidRuntime` crash from `com.domina.cycle`.
Expected: no crash. (The dashboard sits behind the PIN lock; after unlock it shows the ring — with no data yet it shows "WELCOME / —" and the "Log a few periods…" prompt, which is correct.)

- [ ] **Step 5: Run the full suites once more**

Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```
Expected: all pass (Phase 1 instrumented tests still green; Phase 2 added only JVM unit tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/domina/cycle/ui/today/CycleRing.kt \
        app/src/main/java/com/domina/cycle/ui/today/TodayScreen.kt
git commit -m "feat: cycle-ring hero dashboard with phase guidance"
```

---

## Phase 2 Definition of Done

- Prediction engine + period deriver + guidance are pure Kotlin with passing unit tests.
- Full unit suite green; Phase 1 instrumented suite still green on device.
- App builds, installs, and launches without crashing; the Today dashboard shows the cycle ring, next-period & fertile chips, and the current phase's guidance card (or a friendly empty state before enough data is logged).
- No `INTERNET` permission introduced (Phase 2 adds no new permissions).

## Notes / intentionally deferred

- **No cached `Prediction` table or WorkManager recompute yet** — the dashboard computes reactively from the repository Flow. The cached table + nightly recompute land in Phase 3 (notifications), which is when off-screen prediction is actually needed (YAGNI until then).
- BBT/cervical-mucus/LH refinement of ovulation is a TTC-mode enhancement for a later phase; Phase 2 uses the calendar-based estimate (next period − 14).
- A Settings control to override default cycle/period length can come with the Settings screen in a later phase; defaults (28/5) are used until enough history exists.
```
