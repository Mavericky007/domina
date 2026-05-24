# Phase 5: Insights & Analytics — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An Insights tab that visualizes her data — cycle-length history, a BBT chart with an automatically detected coverline, a weight trend (pregnancy), and gentle pattern insights ("cramps tend to show up in your menstrual phase") — all computed on-device.

**Architecture:** Pure-Kotlin analytics (`CycleHistoryStats`, `BbtAnalysis` with a 3-over-6 coverline detector, `PatternInsights`) — exhaustively unit-tested. Charts are lightweight **Compose Canvas** composables (no third-party charting dependency — consistent with the cycle ring, keeps the app dependency-free). A new Insights bottom-nav tab hosts them. Still no networking — **no `INTERNET` permission.**

**Tech Stack:** Kotlin, java.time, Jetpack Compose (Canvas), Hilt. Builds on Phases 1–4b (on `main`). No new dependencies.

> Tasks 1–3 are pure-JVM unit-tested (no device). Tasks 4–5 are UI (need the connected Galaxy S23 for install/launch). **Do NOT run `connectedDebugAndroidTest` except the Task 5 final verify** — it uninstalls the app and erases data; the controller re-seeds after merge.

## Builds on (existing, on `main`)
- `domain/prediction/{PeriodDeriver,LoggedPeriod,CyclePredictor,CyclePhase}.kt`; `data/model/{DayLog,FlowIntensity,Mood}.kt`.
- `data/repository/DayLogRepository.kt` (`observeRange`), `WeightRepository.kt` (`observeAll`).
- `ui/nav/{Destinations,AppNav}.kt` (bottom nav: Today / Calendar / Settings).

## New file structure
```
app/src/main/java/com/domina/cycle/
  domain/insights/
    CycleHistoryStats.kt    # pure: cycle lengths + avg/min/max
    BbtAnalysis.kt          # pure: bbt series + 3-over-6 coverline
    PatternInsights.kt      # pure: textual insights from logs
  ui/insights/
    Charts.kt               # Canvas LineChart (+ optional coverline) and BarChart
    InsightsViewModel.kt    # combines logs + weights into an InsightsUiState (pure buildState)
    InsightsScreen.kt
  ui/nav/{Destinations,AppNav}.kt   # MODIFY: add Insights tab
app/src/test/java/com/domina/cycle/domain/insights/
  CycleHistoryStatsTest.kt, BbtAnalysisTest.kt, PatternInsightsTest.kt
app/src/test/java/com/domina/cycle/ui/insights/InsightsStateTest.kt
```

---

## Task 1: CycleHistoryStats (pure)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/insights/CycleHistoryStats.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/insights/CycleHistoryStatsTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.insights

import com.domina.cycle.domain.prediction.LoggedPeriod
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class CycleHistoryStatsTest {
    private fun p(s: String) = LoggedPeriod(LocalDate.parse(s), 5)

    @Test fun emptyOrSingleHasNoCycleLengths() {
        assertThat(CycleHistoryStats.compute(emptyList()).cycleLengths).isEmpty()
        assertThat(CycleHistoryStats.compute(listOf(p("2026-05-01"))).cycleLengths).isEmpty()
    }

    @Test fun computesLengthsAndStats() {
        val periods = listOf(p("2026-02-01"), p("2026-03-01"), p("2026-03-29"), p("2026-04-30"))
        val s = CycleHistoryStats.compute(periods) // gaps: 28, 28, 32
        assertThat(s.cycleLengths).containsExactly(28, 28, 32).inOrder()
        assertThat(s.average).isEqualTo(29)   // (28+28+32)/3 = 29.33 -> 29
        assertThat(s.shortest).isEqualTo(28)
        assertThat(s.longest).isEqualTo(32)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*CycleHistoryStatsTest*"`
Expected: FAIL — unresolved `CycleHistoryStats`.

- [ ] **Step 3: Implement `CycleHistoryStats`**

```kotlin
package com.domina.cycle.domain.insights

import com.domina.cycle.domain.prediction.LoggedPeriod
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object CycleHistoryStats {
    data class History(val cycleLengths: List<Int>, val average: Int, val shortest: Int, val longest: Int)

    fun compute(periods: List<LoggedPeriod>): History {
        val starts = periods.sortedBy { it.start }.map { it.start }
        val lengths = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        if (lengths.isEmpty()) return History(emptyList(), 0, 0, 0)
        return History(lengths, lengths.average().roundToInt(), lengths.min(), lengths.max())
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*CycleHistoryStatsTest*"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/insights/CycleHistoryStats.kt \
        app/src/test/java/com/domina/cycle/domain/insights/CycleHistoryStatsTest.kt
git commit -m "feat: cycle-history stats (lengths, average, range)"
```

---

## Task 2: BbtAnalysis with 3-over-6 coverline (pure)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/insights/BbtAnalysis.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/insights/BbtAnalysisTest.kt`

Contract: `detectCoverline(temps)` scans for the first index `i >= 6` where the previous 6 temps' max + 0.1 (the coverline) is exceeded by 3 consecutive temps (`i, i+1, i+2`). Returns the coverline value + rise index, or nulls if not found.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.insights

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BbtAnalysisTest {
    @Test fun noRiseReturnsNull() {
        val flat = List(10) { 36.4 }
        val r = BbtAnalysis.detectCoverline(flat)
        assertThat(r.coverline).isNull()
        assertThat(r.riseIndex).isNull()
    }

    @Test fun detectsThreeOverSixShift() {
        // 6 low temps, then a sustained rise
        val temps = listOf(36.4, 36.3, 36.4, 36.5, 36.4, 36.3, 36.7, 36.8, 36.75, 36.7)
        val r = BbtAnalysis.detectCoverline(temps)
        assertThat(r.riseIndex).isEqualTo(6)
        assertThat(r.coverline!!).isWithin(0.001).of(36.6) // max(prev6)=36.5, +0.1
    }

    @Test fun tooFewPointsReturnsNull() {
        assertThat(BbtAnalysis.detectCoverline(listOf(36.4, 36.5)).coverline).isNull()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*BbtAnalysisTest*"`
Expected: FAIL — unresolved `BbtAnalysis`.

- [ ] **Step 3: Implement `BbtAnalysis`**

```kotlin
package com.domina.cycle.domain.insights

object BbtAnalysis {
    data class Coverline(val coverline: Double?, val riseIndex: Int?)

    private const val OFFSET = 0.1

    /** Classic 3-over-6 fertility-awareness coverline detection. Temps in cycle (chronological) order. */
    fun detectCoverline(temps: List<Double>): Coverline {
        for (i in 6..temps.size - 3) {
            val baselineMax = temps.subList(i - 6, i).max()
            val coverline = baselineMax + OFFSET
            if (temps[i] > coverline && temps[i + 1] > coverline && temps[i + 2] > coverline) {
                return Coverline(coverline, i)
            }
        }
        return Coverline(null, null)
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*BbtAnalysisTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/insights/BbtAnalysis.kt \
        app/src/test/java/com/domina/cycle/domain/insights/BbtAnalysisTest.kt
git commit -m "feat: BBT 3-over-6 coverline detection"
```

---

## Task 3: PatternInsights (pure)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/insights/PatternInsights.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/insights/PatternInsightsTest.kt`

Contract: `generate(logs, periods, averageCycle)` returns warm, plain-language insight strings. For each symptom, it finds which cycle phase it most often falls in (phase computed from day-of-cycle relative to the most recent period start on/before each log's date) and, if there's a clear majority, emits "‹symptom› tend to show up in your ‹phase› phase". Also emits a cycle-length summary when available. Empty data → empty list.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.insights

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.domain.prediction.LoggedPeriod
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PatternInsightsTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun emptyDataGivesNoInsights() {
        assertThat(PatternInsights.generate(emptyList(), emptyList(), 28)).isEmpty()
    }

    @Test fun symptomConcentratedInMenstrualPhaseIsReported() {
        // period started May 1; cramps logged on days 1-3 (menstrual phase)
        val periods = listOf(LoggedPeriod(d("2026-05-01"), 5))
        val logs = listOf(
            DayLog(date = d("2026-05-01"), symptoms = listOf("cramps")),
            DayLog(date = d("2026-05-02"), symptoms = listOf("cramps")),
            DayLog(date = d("2026-05-03"), symptoms = listOf("cramps")),
        )
        val insights = PatternInsights.generate(logs, periods, 28)
        assertThat(insights.any { it.contains("cramps") && it.contains("menstrual", ignoreCase = true) }).isTrue()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PatternInsightsTest*"`
Expected: FAIL — unresolved `PatternInsights`.

- [ ] **Step 3: Implement `PatternInsights`**

```kotlin
package com.domina.cycle.domain.insights

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.domain.prediction.CyclePhase
import com.domina.cycle.domain.prediction.LoggedPeriod
import java.time.temporal.ChronoUnit

object PatternInsights {
    private const val LUTEAL = 14

    fun generate(logs: List<DayLog>, periods: List<LoggedPeriod>, averageCycle: Int): List<String> {
        if (logs.isEmpty() || periods.isEmpty()) return emptyList()
        val starts = periods.map { it.start }.sorted()
        val out = mutableListOf<String>()

        // symptom -> phase tally
        val tally = HashMap<String, HashMap<CyclePhase, Int>>()
        logs.forEach { log ->
            val start = starts.lastOrNull { !it.isAfter(log.date) } ?: return@forEach
            val day = ChronoUnit.DAYS.between(start, log.date).toInt() + 1
            val phase = phaseOf(day, averageCycle)
            log.symptoms.forEach { sym ->
                tally.getOrPut(sym) { HashMap() }.merge(phase, 1, Int::plus)
            }
        }
        tally.forEach { (sym, phases) ->
            val total = phases.values.sum()
            val (topPhase, topCount) = phases.maxByOrNull { it.value }!!
            if (total >= 3 && topCount * 2 > total) { // clear majority over >=3 occurrences
                out += "$sym tend to show up in your ${topPhase.label()} phase 💛"
            }
        }
        return out
    }

    private fun phaseOf(day: Int, cycle: Int): CyclePhase {
        val ovulationDay = cycle - LUTEAL
        return when {
            day <= 5 -> CyclePhase.MENSTRUAL
            day in (ovulationDay - 1)..(ovulationDay + 1) -> CyclePhase.OVULATION
            day < ovulationDay - 1 -> CyclePhase.FOLLICULAR
            else -> CyclePhase.LUTEAL
        }
    }

    private fun CyclePhase.label() = name.lowercase()
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PatternInsightsTest*"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/insights/PatternInsights.kt \
        app/src/test/java/com/domina/cycle/domain/insights/PatternInsightsTest.kt
git commit -m "feat: on-device pattern insights (symptom-by-phase)"
```

---

## Task 4: Canvas chart composables

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/insights/Charts.kt`

- [ ] **Step 1: Implement `Charts.kt` (LineChart with optional coverline, BarChart)**

```kotlin
package com.domina.cycle.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    coverline: Float? = null,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    coverlineColor: Color = MaterialTheme.colorScheme.secondary,
) {
    if (values.size < 2) { Text("Not enough data yet 💛", style = MaterialTheme.typography.bodySmall); return }
    val allVals = values + listOfNotNull(coverline)
    val minV = allVals.min(); val maxV = allVals.max(); val range = (maxV - minV).takeIf { it > 0f } ?: 1f
    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width; val h = size.height; val pad = 8f
        fun x(i: Int) = pad + (w - 2 * pad) * (i.toFloat() / (values.size - 1))
        fun y(v: Float) = h - pad - (h - 2 * pad) * ((v - minV) / range)
        coverline?.let { cl ->
            val cy = y(cl)
            drawLine(coverlineColor, Offset(pad, cy), Offset(w - pad, cy), strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)))
        }
        for (i in 0 until values.size - 1) {
            drawLine(lineColor, Offset(x(i), y(values[i])), Offset(x(i + 1), y(values[i + 1])), strokeWidth = 6f)
        }
        values.forEachIndexed { i, v -> drawCircle(lineColor, radius = 7f, center = Offset(x(i), y(v))) }
    }
}

@Composable
fun BarChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (values.isEmpty()) { Text("Not enough data yet 💛", style = MaterialTheme.typography.bodySmall); return }
    val maxV = (values.max()).coerceAtLeast(1)
    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width; val h = size.height; val pad = 8f
        val slot = (w - 2 * pad) / values.size
        val barW = slot * 0.6f
        values.forEachIndexed { i, v ->
            val bh = (h - 2 * pad) * (v.toFloat() / maxV)
            val left = pad + i * slot + (slot - barW) / 2
            drawRect(barColor, topLeft = Offset(left, h - pad - bh), size = androidx.compose.ui.geometry.Size(barW, bh))
        }
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/domina/cycle/ui/insights/Charts.kt
git commit -m "feat: lightweight Canvas line & bar charts"
```

---

## Task 5: Insights screen + ViewModel + nav tab

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/insights/InsightsViewModel.kt`, `InsightsScreen.kt`
- Modify: `ui/nav/Destinations.kt`, `AppNav.kt`
- Test: `app/src/test/java/com/domina/cycle/ui/insights/InsightsStateTest.kt`

- [ ] **Step 1: Write the failing test for the pure state builder**

```kotlin
package com.domina.cycle.ui.insights

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.db.entity.WeightEntryEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class InsightsStateTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun flow(date: String) = DayLog(date = d(date), flow = FlowIntensity.MEDIUM)

    @Test fun buildsCycleLengthsAndWeightSeriesFromData() {
        val logs = listOf(flow("2026-02-01"), flow("2026-03-01"), flow("2026-03-29"))
        val weights = listOf(
            WeightEntryEntity(dateEpochDay = d("2026-05-01").toEpochDay(), weightKg = 64.0),
            WeightEntryEntity(dateEpochDay = d("2026-05-15").toEpochDay(), weightKg = 65.0),
        )
        val s = InsightsViewModel.buildState(logs, weights)
        assertThat(s.cycleLengths).containsExactly(28, 28).inOrder()
        assertThat(s.weightSeries).containsExactly(64.0f, 65.0f).inOrder()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*InsightsStateTest*"`
Expected: FAIL — `InsightsViewModel.buildState` not found.

- [ ] **Step 3: Implement `InsightsViewModel`**

```kotlin
package com.domina.cycle.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.WeightEntryEntity
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.data.repository.WeightRepository
import com.domina.cycle.domain.insights.BbtAnalysis
import com.domina.cycle.domain.insights.CycleHistoryStats
import com.domina.cycle.domain.insights.PatternInsights
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class InsightsUiState(
    val cycleLengths: List<Int>,
    val averageCycle: Int,
    val shortest: Int,
    val longest: Int,
    val bbtSeries: List<Float>,
    val coverline: Float?,
    val weightSeries: List<Float>,
    val insights: List<String>,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    dayLogRepository: DayLogRepository,
    weightRepository: WeightRepository,
) : ViewModel() {
    private val today: LocalDate = LocalDate.now()

    val state: StateFlow<InsightsUiState> =
        combine(
            dayLogRepository.observeRange(today.minusDays(400), today),
            weightRepository.observeAll(),
        ) { logs, weights -> buildState(logs, weights) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(emptyList(), emptyList()))

    companion object {
        fun buildState(logs: List<DayLog>, weights: List<WeightEntryEntity>): InsightsUiState {
            val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
            val history = CycleHistoryStats.compute(periods)
            val avgCycle = if (history.average > 0) history.average else CyclePredictor.DEFAULT_CYCLE_LENGTH
            val bbt = logs.filter { it.bbt != null }.sortedBy { it.date }.map { it.bbt!! }
            val cover = BbtAnalysis.detectCoverline(bbt)
            return InsightsUiState(
                cycleLengths = history.cycleLengths,
                averageCycle = history.average,
                shortest = history.shortest,
                longest = history.longest,
                bbtSeries = bbt.map { it.toFloat() },
                coverline = cover.coverline?.toFloat(),
                weightSeries = weights.sortedBy { it.dateEpochDay }.map { it.weightKg.toFloat() },
                insights = PatternInsights.generate(logs, periods, avgCycle),
            )
        }
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*InsightsStateTest*"`
Expected: PASS.

- [ ] **Step 5: Implement `InsightsScreen`**

```kotlin
package com.domina.cycle.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun InsightsScreen(vm: InsightsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))
        SectionCard("Cycle length history") {
            BarChart(values = s.cycleLengths)
            if (s.cycleLengths.isNotEmpty()) {
                Text("Average ${s.averageCycle} days · ${s.shortest}–${s.longest} day range",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }

        SectionCard("Basal body temperature") {
            LineChart(values = s.bbtSeries, coverline = s.coverline)
            if (s.coverline != null) {
                Text("Coverline detected — a temperature shift suggests ovulation has passed.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        SectionCard("Weight") { LineChart(values = s.weightSeries) }

        SectionCard("Patterns") {
            if (s.insights.isEmpty()) Text("Keep logging and I'll spot patterns for you 💛",
                style = MaterialTheme.typography.bodyMedium)
            else s.insights.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
```

- [ ] **Step 6: Add the Insights bottom-nav tab**

In `Destinations.kt` add `const val INSIGHTS = "insights"`.
In `AppNav.kt`: add a `NavigationBarItem` for Insights (use `Icons.Filled.Insights` from material-icons-extended; `selected = currentRoute == Destinations.INSIGHTS`, `onClick = { nav.navigate(Destinations.INSIGHTS) }`, `colors = navColors`) between Calendar and Settings, and register `composable(Destinations.INSIGHTS) { InsightsScreen() }`.

- [ ] **Step 7: Build, full unit suite, install, verify on device**

Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
adb shell pm list packages | grep domina
```
Expected: unit suite all pass (incl. the new analytics tests); instrumented suite all pass; app installed. On device: open the **Insights** tab → cycle-length bars, BBT chart (with coverline if BBT logged), weight line, and any pattern insights appear; with sparse data the friendly empty states show.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/domina/cycle/ui/insights/InsightsViewModel.kt \
        app/src/main/java/com/domina/cycle/ui/insights/InsightsScreen.kt \
        app/src/main/java/com/domina/cycle/ui/nav/ \
        app/src/test/java/com/domina/cycle/ui/insights/InsightsStateTest.kt
git commit -m "feat: Insights tab with cycle/BBT/weight charts and pattern insights"
```

---

## Phase 5 Definition of Done

- `CycleHistoryStats`, `BbtAnalysis` (3-over-6 coverline), `PatternInsights`, and `InsightsViewModel.buildState` are pure Kotlin with passing unit tests.
- An Insights bottom-nav tab shows cycle-length bars, a BBT line chart with auto-detected coverline, a weight trend line, and pattern insight cards — with friendly empty states for sparse data.
- Full unit + instrumented suites green; app installed at the end. Still **no `INTERNET` permission**; no new dependencies added.

## Deferred (later)
- X-axis date labels / richer chart axes & tooltips; symptom-frequency charts; exporting charts into the doctor-report PDF (Phase 6).
```
