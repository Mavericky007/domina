# Calendar, Pregnancy & Period-Reminder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make cycle predictions mode-aware (no period/ovulation forecasts in pregnancy mode), show an intimacy indicator and a color-coded trimester overlay on the calendar, and add twice-daily period-logging notifications that log flow with one tap.

**Architecture:** A new pure `CycleForecast` gate wraps the existing predictors and returns empty marks/outlook in pregnancy mode; `CalendarViewModel`, `TodayViewModel`, and `ReminderManager` all consume it. A new pure `PregnancyProjection` maps month days → trimester. A new pure `PeriodLogPrompt` decides when to nudge. Period reminders reuse the existing check-in alarm→receiver→action-receiver pattern verbatim.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Hilt, DataStore, AlarmManager + BroadcastReceiver, JUnit + Truth.

**Spec:** `docs/superpowers/specs/2026-05-25-calendar-pregnancy-period-reminders-design.md`

**Conventions:** Tests live under `app/src/test/java/com/domina/cycle/...`, use `import com.google.common.truth.Truth.assertThat` and `org.junit.Test`. Build: `./gradlew :app:assembleDebug`. Test: `./gradlew :app:testDebugUnitTest`. Single test class: `./gradlew :app:testDebugUnitTest --tests "com.domina.cycle.<FQN>"`.

---

## File Structure

**Create:**
- `app/src/main/java/com/domina/cycle/domain/prediction/CycleForecast.kt` — mode gate over marks + outlook
- `app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyProjection.kt` — month day → trimester
- `app/src/main/java/com/domina/cycle/domain/reminders/PeriodLogPrompt.kt` — when to nudge
- `app/src/main/java/com/domina/cycle/reminders/PeriodReminderScheduler.kt`
- `app/src/main/java/com/domina/cycle/reminders/PeriodReminderReceiver.kt`
- `app/src/main/java/com/domina/cycle/reminders/PeriodLogActionReceiver.kt`
- Tests: `CycleForecastTest.kt`, `PregnancyProjectionTest.kt`, `PeriodLogPromptTest.kt` (mirrored package paths)

**Modify:**
- `ui/calendar/CalendarViewModel.kt` — mode/dueDate in data; route via `CycleForecast`; `intimacyDays`; trimester marks
- `ui/calendar/CalendarScreen.kt` — ❤️ overlay, trimester tints, trimester list headers, legend swap; updated `buildState` calls
- `ui/today/TodayViewModel.kt` — outlook via `CycleForecast`
- `reminders/ReminderManager.kt` — skip period/fertility + period-log reminders in pregnancy mode
- `domain/reminders/ReminderSettings.kt` — add `periodLogReminders`
- `data/prefs/SettingsRepository.kt` — persist `periodLogReminders`
- `ui/settings/SettingsScreen.kt` — toggle row
- `reminders/Notifier.kt` — `notifyPeriodLog` + confirmation
- `reminders/NotificationChannels.kt` — `CYCLE_LOG` channel
- `reminders/ReceiverEntryPoint.kt` — expose `dayLogRepository()`
- `app/src/main/AndroidManifest.xml` — register the two new receivers

---

## Task 1: CycleForecast — mode gate (items 1 & 3 foundation)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/prediction/CycleForecast.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/prediction/CycleForecastTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.prediction

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.pregnancy.AppMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CycleForecastTest {
    private val periods = listOf(LoggedPeriod(LocalDate.parse("2026-05-16"), 5))
    private val today = LocalDate.parse("2026-05-24")
    private val logs = listOf(DayLog(LocalDate.parse("2026-05-16"), flow = FlowIntensity.MEDIUM))
    private val prediction = CyclePredictor.predict(periods, today)

    @Test fun pregnancyModeProducesNoMarks() {
        val m = CycleForecast.marksFor(AppMode.PREGNANCY, periods, YearMonth.of(2026, 6), 28, 5, today)
        assertThat(m.predictedPeriod).isEmpty()
        assertThat(m.fertile).isEmpty()
        assertThat(m.ovulation).isEmpty()
    }

    @Test fun pregnancyModeOutlookIsEmpty() {
        val o = CycleForecast.outlookFor(AppMode.PREGNANCY, prediction, periods, logs, today)
        assertThat(o).isEqualTo(CycleRisk.Outlook.EMPTY)
    }

    @Test fun cycleModeDelegatesToProjection() {
        val gated = CycleForecast.marksFor(AppMode.CYCLE, periods, YearMonth.of(2026, 6), 28, 5, today)
        val direct = CycleProjection.marksFor(periods, YearMonth.of(2026, 6), 28, 5, today)
        assertThat(gated).isEqualTo(direct)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.domina.cycle.domain.prediction.CycleForecastTest"`
Expected: FAIL — `CycleForecast` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.domina.cycle.domain.prediction

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.domain.pregnancy.AppMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * Mode-aware gate over the cycle predictors. In pregnancy mode there is no menstrual cycle to
 * forecast, so period/fertile/ovulation marks and the cycle-risk outlook are suppressed. Cycle and
 * TTC modes pass straight through (TTC intentionally keeps fertile/ovulation).
 */
object CycleForecast {
    fun marksFor(
        mode: AppMode,
        periods: List<LoggedPeriod>,
        month: YearMonth,
        avgCycle: Int,
        avgPeriod: Int,
        today: LocalDate,
        delayDays: Int = 0,
    ): CycleProjection.MonthMarks =
        if (mode == AppMode.PREGNANCY) CycleProjection.MonthMarks()
        else CycleProjection.marksFor(periods, month, avgCycle, avgPeriod, today, delayDays)

    fun outlookFor(
        mode: AppMode,
        prediction: CyclePrediction,
        periods: List<LoggedPeriod>,
        logs: List<DayLog>,
        today: LocalDate,
    ): CycleRisk.Outlook =
        if (mode == AppMode.PREGNANCY) CycleRisk.Outlook.EMPTY
        else CycleRisk.analyze(prediction, periods, logs, today)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.domina.cycle.domain.prediction.CycleForecastTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/prediction/CycleForecast.kt app/src/test/java/com/domina/cycle/domain/prediction/CycleForecastTest.kt
git commit -m "feat(prediction): CycleForecast mode gate (no cycle forecasts in pregnancy)"
```

---

## Task 2: Calendar uses mode + CycleForecast (items 1 & 3 for Calendar)

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/calendar/CalendarViewModel.kt`
- Modify: `app/src/main/java/com/domina/cycle/ui/calendar/CalendarScreen.kt`

- [ ] **Step 1: Thread mode + dueDate through `CalendarData` and `buildState`.**

In `CalendarViewModel.kt`:
- Add imports: `import com.domina.cycle.data.prefs.SettingsRepository`, `import com.domina.cycle.domain.pregnancy.AppMode`, `import com.domina.cycle.domain.prediction.CycleForecast`, `import kotlinx.coroutines.flow.combine`, `import java.time.LocalDate` (already present).
- Inject settings: change constructor to
  `class CalendarViewModel @Inject constructor(repository: DayLogRepository, settings: SettingsRepository,)`.
- Extend `CalendarData`:
  ```kotlin
  data class CalendarData(
      val logs: List<DayLog>,
      val today: LocalDate,
      val mode: AppMode = AppMode.CYCLE,
      val dueDate: LocalDate? = null,
  )
  ```
- Replace the `data` flow with a combine:
  ```kotlin
  val data: StateFlow<CalendarData> =
      combine(
          repository.observeRange(today.minusDays(400), today.plusDays(400)),
          settings.appMode,
          settings.dueDate,
      ) { logs, mode, due -> CalendarData(logs, today, mode, due) }
          .stateIn(
              viewModelScope, SharingStarted.WhileSubscribed(5000),
              CalendarData(emptyList(), today),
          )
  ```
- Change `buildState` signature to
  `fun buildState(m: YearMonth, logs: List<DayLog>, today: LocalDate, mode: AppMode, dueDate: LocalDate?): CalendarUiState`
  and inside it replace the marks line:
  ```kotlin
  val marks = CycleForecast.marksFor(
      mode, periods, m, pred.averageCycleLength, pred.averagePeriodLength, today, outlook.delayDays,
  )
  ```
  and the outlook line:
  ```kotlin
  val outlook = CycleForecast.outlookFor(mode, pred, periods, logs, today)
  ```

- [ ] **Step 2: Update the two `buildState` call sites in `CalendarScreen.kt`.**

```kotlin
val currentState = remember(currentMonth, data) {
    CalendarViewModel.buildState(currentMonth, data.logs, data.today, data.mode, data.dueDate)
}
```
and inside the `HorizontalPager`:
```kotlin
val st = remember(page, data) {
    CalendarViewModel.buildState(monthOf(page), data.logs, data.today, data.mode, data.dueDate)
}
```

- [ ] **Step 3: Build to verify it compiles.**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/domina/cycle/ui/calendar/CalendarViewModel.kt app/src/main/java/com/domina/cycle/ui/calendar/CalendarScreen.kt
git commit -m "fix(calendar): no period/ovulation forecasts in pregnancy mode (via CycleForecast)"
```

---

## Task 3: Today + ReminderManager respect mode

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/today/TodayViewModel.kt`
- Modify: `app/src/main/java/com/domina/cycle/reminders/ReminderManager.kt`

- [ ] **Step 1: Route Today's outlook through `CycleForecast`.**

In `TodayViewModel.kt`:
- Add import `import com.domina.cycle.domain.prediction.CycleForecast`.
- Change `buildState` signature to `fun buildState(logs: List<DayLog>, today: LocalDate, mode: AppMode): TodayUiState` and replace the outlook line with
  `val outlook = CycleForecast.outlookFor(mode, prediction, periods, logs, today)`.
- Update `TodayUiState.empty` to pass a mode: change to `fun empty(today: LocalDate) = TodayUiState(...)` unchanged (outlook default already `Outlook.EMPTY`).
- Change the `state` flow to combine mode:
  ```kotlin
  val state: StateFlow<TodayUiState> =
      combine(
          repository.observeRange(today.minusDays(LOOKBACK_DAYS), today),
          settings.appMode,
      ) { logs, mode -> buildState(logs, mode, today) }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.empty(today))
  ```
  (NOTE: pass args in the order your final `buildState` declares — keep `buildState(logs, today, mode)` consistent; pick one order and use it in both the signature and the call.)

- [ ] **Step 2: Gate notifications in `ReminderManager`.**

In `reminders/ReminderManager.kt`, after `val settings: ReminderSettings = settingsProvider.current()` add a mode read and gate the period/fertility scheduling. The `ReminderSettingsProvider` only yields `ReminderSettings`; read the mode from the injected `DayLogRepository`? No — inject `SettingsRepository`. Add constructor param `private val appSettings: com.domina.cycle.data.prefs.SettingsRepository,` and:
```kotlin
val mode = appSettings.appMode.first()
val cycleReminders = if (mode == com.domina.cycle.domain.pregnancy.AppMode.PREGNANCY)
    ReminderSettings(periodAlerts = false, fertilityAlerts = false,
        dailyNudge = settings.dailyNudge, dailyNudgeTime = settings.dailyNudgeTime,
        checkIns = settings.checkIns)
else settings
val reminders = ReminderScheduler.compute(prediction, cycleReminders, LocalDateTime.now())
AlarmScheduler(context).reschedule(reminders)
```
(Keeps the daily nudge & check-ins; suppresses period/fertility in pregnancy.)

- [ ] **Step 3: Build.**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run unit tests (TodayUiState test may need the new arg).**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. If `TodayUiStateTest` calls `buildState(logs, today)`, update those calls to `buildState(logs, today, AppMode.CYCLE)` (add `import com.domina.cycle.domain.pregnancy.AppMode`).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix(today+reminders): respect pregnancy mode (no cycle outlook / period-fertility alerts)"
```

---

## Task 4: Intimacy ❤️ indicator on the grid (item 2)

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/calendar/CalendarViewModel.kt`
- Modify: `app/src/main/java/com/domina/cycle/ui/calendar/CalendarScreen.kt`

- [ ] **Step 1: Add `intimacyDays` to state + buildState.**

In `CalendarViewModel.kt`, add to `CalendarUiState`: `val intimacyDays: Set<Int> = emptySet(),`. In `buildState`, after `loggedDays`:
```kotlin
val intimacyDays = logs
    .filter { YearMonth.from(it.date) == m && it.intimacy != com.domina.cycle.data.model.Intimacy.NONE }
    .map { it.date.dayOfMonth }.toSet()
```
and include `intimacyDays = intimacyDays,` in the returned `CalendarUiState`.

- [ ] **Step 2: Overlay a tiny ❤️ in `DayCell` (CalendarScreen.kt).**

Replace the inner `Box { Text(...) }` of `DayCell` so the heart sits bottom-center without changing the cell size:
```kotlin
Box(
    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick).background(fill).then(borderMod),
    contentAlignment = Alignment.Center,
) {
    Text(
        "$day", style = MaterialTheme.typography.bodyMedium, color = onFill,
        fontWeight = if (isToday || isPeriod || isOvulation) FontWeight.Bold else FontWeight.Normal,
    )
    if (day in s.intimacyDays) {
        Text("❤️", fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp))
    }
}
```
Add imports: `import androidx.compose.ui.unit.sp`.

- [ ] **Step 3: Add a legend entry.** In the cycle `FlowRow` legend add:
```kotlin
LegendHeart("Intimacy")
```
and a helper near `LegendDot`:
```kotlin
@Composable
private fun LegendHeart(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("❤️", fontSize = 10.sp)
        Text(" $label", style = MaterialTheme.typography.bodySmall)
    }
}
```

- [ ] **Step 4: Build.**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(calendar): tiny heart indicator on days with logged intimacy"
```

---

## Task 5: PregnancyProjection (item 4 foundation)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyProjection.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/pregnancy/PregnancyProjectionTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.pregnancy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PregnancyProjectionTest {
    // due = 2026-12-31 -> start (LMP) = due - 280 = 2026-03-26
    private val due = LocalDate.parse("2026-12-31")

    @Test fun startMonthIsTrimester1() {
        val m = PregnancyProjection.marksFor(due, YearMonth.of(2026, 3))
        // 2026-03-26 is week 0 -> trimester 1
        assertThat(m[26]).isEqualTo(1)
        assertThat(m[25]).isNull() // before conception/LMP span
    }

    @Test fun lateMonthsAreTrimester3() {
        val m = PregnancyProjection.marksFor(due, YearMonth.of(2026, 12))
        assertThat(m[31]).isEqualTo(3) // due date
        assertThat(m[1]).isEqualTo(3)
    }

    @Test fun outsideSpanIsEmpty() {
        assertThat(PregnancyProjection.marksFor(due, YearMonth.of(2026, 1))).isEmpty()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.domina.cycle.domain.pregnancy.PregnancyProjectionTest"`
Expected: FAIL — unresolved `PregnancyProjection`.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.domina.cycle.domain.pregnancy

import java.time.LocalDate
import java.time.YearMonth

/** Maps each day of [month] that falls within the pregnancy span to its trimester (1, 2 or 3). */
object PregnancyProjection {
    private const val TERM_DAYS = 280L

    fun marksFor(dueDate: LocalDate, month: YearMonth): Map<Int, Int> {
        val start = dueDate.minusDays(TERM_DAYS) // LMP / week 0
        val result = HashMap<Int, Int>()
        var day = month.atDay(1)
        val end = month.atEndOfMonth()
        while (!day.isAfter(end)) {
            if (!day.isBefore(start) && !day.isAfter(dueDate)) {
                val weeksCompleted = (java.time.temporal.ChronoUnit.DAYS.between(start, day) / 7).toInt()
                result[day.dayOfMonth] = when {
                    weeksCompleted <= 13 -> 1
                    weeksCompleted <= 27 -> 2
                    else -> 3
                }
            }
            day = day.plusDays(1)
        }
        return result
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.domina.cycle.domain.pregnancy.PregnancyProjectionTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyProjection.kt app/src/test/java/com/domina/cycle/domain/pregnancy/PregnancyProjectionTest.kt
git commit -m "feat(pregnancy): PregnancyProjection maps month days to trimester"
```

---

## Task 6: Trimester overlay on calendar grid + list (item 4)

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/calendar/CalendarViewModel.kt`
- Modify: `app/src/main/java/com/domina/cycle/ui/calendar/CalendarScreen.kt`

- [ ] **Step 1: Add trimester marks + list events in `buildState`.**

In `CalendarUiState` add: `val trimesterDays: Map<Int, Int> = emptyMap(), val isPregnancy: Boolean = false,`.
In `buildState`, compute (uses the `mode`/`dueDate` params from Task 2):
```kotlin
val isPregnancy = mode == AppMode.PREGNANCY && dueDate != null
val trimesterDays = if (isPregnancy) PregnancyProjection.marksFor(dueDate!!, m) else emptyMap()
```
Add import `import com.domina.cycle.domain.pregnancy.PregnancyProjection`.
Include `trimesterDays = trimesterDays, isPregnancy = isPregnancy,` in the returned state.

For the **list**, when `isPregnancy`, prepend trimester/due events instead of the cycle markers. Inside `buildState`, replace `markerEvents` usage in the `all` list when pregnant:
```kotlin
val markerEvents = if (isPregnancy) buildList {
    // due date marker if it's this month
    if (YearMonth.from(dueDate) == m) {
        add(CalendarEvent(dueDate, "👶", "Due date", dueDate.format(dayFmt), CalEventKind.NOTE, dueDate))
    }
    // first day of each trimester visible this month
    trimesterDays.entries.groupBy { it.value }.forEach { (tri, days) ->
        val firstDay = days.minOf { it.key }
        val date = m.atDay(firstDay)
        add(CalendarEvent(null, triEmoji(tri), "Trimester $tri", "Week ${weekOf(dueDate, date)}+", CalEventKind.NOTE, date))
    }
} else { /* existing cycle markerEvents block unchanged */ }
```
Add helpers in the companion:
```kotlin
private fun triEmoji(t: Int) = when (t) { 1 -> "🌱"; 2 -> "🌷"; else -> "🌳" }
private fun weekOf(due: LocalDate, date: LocalDate): Int =
    (java.time.temporal.ChronoUnit.DAYS.between(due.minusDays(280), date) / 7).toInt()
```

- [ ] **Step 2: Tint grid cells by trimester in `DayCell`.**

In `CalendarScreen.kt` `DayCell`, before computing `fill`, add trimester tint that wins when pregnant:
```kotlin
val triTint = s.trimesterDays[day]?.let { tri ->
    when (tri) { 1 -> cs.tertiaryContainer.copy(alpha = 0.35f)
                 2 -> cs.secondaryContainer.copy(alpha = 0.40f)
                 else -> cs.primaryContainer.copy(alpha = 0.40f) }
}
val fill = when {
    triTint != null -> triTint
    isPeriod -> cs.secondaryContainer
    isOvulation -> cs.tertiary
    isFertile -> cs.tertiaryContainer
    else -> cs.surfaceContainerHigh
}
```
(When pregnant, Task 2 already made period/fertile/ovulation sets empty, so only `triTint` applies.)

- [ ] **Step 3: Swap the legend when pregnant.**

In `CalendarScreen`, replace the static cycle `FlowRow` legend with a conditional:
```kotlin
FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    if (currentState.isPregnancy) {
        LegendDot("Trimester 1", cs.tertiaryContainer, filled = true)
        LegendDot("Trimester 2", cs.secondaryContainer, filled = true)
        LegendDot("Trimester 3", cs.primaryContainer, filled = true)
        LegendHeart("Intimacy")
        LegendDot("Today", cs.primary, filled = false)
    } else {
        LegendDot("Period", cs.secondaryContainer, filled = true)
        LegendDot("Predicted", cs.secondary, filled = false)
        LegendDot("Fertile", cs.tertiaryContainer, filled = true)
        LegendDot("Ovulation", cs.tertiary, filled = true)
        LegendHeart("Intimacy")
        LegendDot("Today", cs.primary, filled = false)
    }
}
```

- [ ] **Step 4: Build + tests.**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(calendar): color-coded trimester overlay + trimester/due list rows in pregnancy mode"
```

---

## Task 7: PeriodLogPrompt (item 5 foundation)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/reminders/PeriodLogPrompt.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/reminders/PeriodLogPromptTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.reminders

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.LoggedPeriod
import com.domina.cycle.domain.prediction.PeriodDeriver
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PeriodLogPromptTest {
    // Steady 28-day history so the predictor yields a confident next-period date.
    private fun history(starts: List<String>, len: Int = 5): Pair<List<DayLog>, List<LoggedPeriod>> {
        val logs = starts.flatMap { s ->
            val d = LocalDate.parse(s)
            (0 until len).map { DayLog(d.plusDays(it.toLong()), flow = FlowIntensity.MEDIUM) }
        }
        val periods = PeriodDeriver.derive(logs.map { it.date })
        return logs to periods
    }

    @Test fun promptsOnPredictedStartWhenNothingLoggedYet() {
        val (logs, periods) = history(listOf("2026-01-01", "2026-01-29", "2026-02-26", "2026-03-26"))
        val today = LocalDate.parse("2026-04-23") // ~ predicted next start
        val pred = CyclePredictor.predict(periods, today)
        // No flow logged on/after the predicted start.
        assertThat(PeriodLogPrompt.shouldPromptOn(today, logs, pred, periods)).isTrue()
    }

    @Test fun skipsWhenTodayAlreadyLogged() {
        val (logs0, _) = history(listOf("2026-01-01", "2026-01-29", "2026-02-26"))
        val today = LocalDate.parse("2026-03-26")
        val logs = logs0 + DayLog(today, flow = FlowIntensity.LIGHT)
        val periods = PeriodDeriver.derive(logs.map { it.date })
        val pred = CyclePredictor.predict(periods, today)
        assertThat(PeriodLogPrompt.shouldPromptOn(today, logs, pred, periods)).isFalse()
    }

    @Test fun outOfWindowDoesNotPrompt() {
        val (logs, periods) = history(listOf("2026-01-01", "2026-01-29", "2026-02-26"))
        val today = LocalDate.parse("2026-03-10") // mid-cycle, no period due
        val pred = CyclePredictor.predict(periods, today)
        assertThat(PeriodLogPrompt.shouldPromptOn(today, logs, pred, periods)).isFalse()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.domina.cycle.domain.reminders.PeriodLogPromptTest"`
Expected: FAIL — unresolved `PeriodLogPrompt`.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.domina.cycle.domain.reminders

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.prediction.CyclePrediction
import com.domina.cycle.domain.prediction.LoggedPeriod
import java.time.LocalDate

/**
 * Decides whether to nudge the user to log her period today. The window opens at the predicted
 * next start and re-anchors to the actual start once a flow day is logged; it runs for an adaptive
 * number of days (her average period length, default 7, clamped 3..10). A day is skipped if its
 * flow is already logged.
 */
object PeriodLogPrompt {
    private const val DEFAULT_LENGTH = 7
    private const val MIN_LENGTH = 3
    private const val MAX_LENGTH = 10
    private const val PREDICTED_GRACE = 3 // tolerate a period arriving a few days late

    data class Window(val start: LocalDate, val endExclusive: LocalDate)

    private fun length(prediction: CyclePrediction): Int =
        prediction.averagePeriodLength.takeIf { it in MIN_LENGTH..MAX_LENGTH } ?: DEFAULT_LENGTH

    fun windowFor(today: LocalDate, periods: List<LoggedPeriod>, prediction: CyclePrediction): Window? {
        val len = length(prediction).toLong()
        // Re-anchor to an actual recent start if we're inside it.
        val actual = periods.maxByOrNull { it.start }?.start
        if (actual != null && !today.isBefore(actual) && today.isBefore(actual.plusDays(len))) {
            return Window(actual, actual.plusDays(len))
        }
        // Otherwise the predicted start opens the window (with a late-arrival grace).
        val predicted = prediction.nextPeriodDate ?: return null
        val end = predicted.plusDays(len + PREDICTED_GRACE)
        if (!today.isBefore(predicted) && today.isBefore(end)) return Window(predicted, end)
        return null
    }

    fun shouldPromptOn(
        today: LocalDate,
        logs: List<DayLog>,
        prediction: CyclePrediction,
        periods: List<LoggedPeriod>,
    ): Boolean {
        val w = windowFor(today, periods, prediction) ?: return false
        if (today.isBefore(w.start) || !today.isBefore(w.endExclusive)) return false
        val loggedToday = logs.any { it.date == today && it.flow != FlowIntensity.NONE }
        return !loggedToday
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.domina.cycle.domain.reminders.PeriodLogPromptTest"`
Expected: PASS. (If `CyclePredictor` does not produce a `nextPeriodDate` for 3 periods, the predicted-start test uses 4 — keep ≥3 cycles of history as written.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/reminders/PeriodLogPrompt.kt app/src/test/java/com/domina/cycle/domain/reminders/PeriodLogPromptTest.kt
git commit -m "feat(reminders): PeriodLogPrompt window (predicted start, re-anchor, adaptive length)"
```

---

## Task 8: Period-reminder setting + Settings toggle (item 5)

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/domain/reminders/ReminderSettings.kt`
- Modify: `app/src/main/java/com/domina/cycle/data/prefs/SettingsRepository.kt`
- Modify: `app/src/main/java/com/domina/cycle/ui/settings/SettingsScreen.kt`
- Test: `app/src/test/java/com/domina/cycle/data/prefs/ReminderSettingsMappingTest.kt` (update if it asserts field set)

- [ ] **Step 1: Add the field (default ON).**

In `ReminderSettings.kt` add `val periodLogReminders: Boolean = true,` (place after `checkIns`).

- [ ] **Step 2: Persist it in `SettingsRepository`.**

Add a key near the other reminder keys: `private val periodLogKey = booleanPreferencesKey("rem_period_log")`.
In the `reminderSettings` `map { p -> ReminderSettings(...) }` add `periodLogReminders = p[periodLogKey] ?: true,`.
In `setReminderSettings`'s `edit { }` add `it[periodLogKey] = s.periodLogReminders`.

- [ ] **Step 3: Add the toggle to Settings UI.**

In `SettingsScreen.kt`, under the "Reminders" section (after the check-ins `ToggleRow`):
```kotlin
ToggleRow("Period logging reminders (2×/day during your period)", rem.periodLogReminders) {
    vm.updateReminders(rem.copy(periodLogReminders = it))
}
```

- [ ] **Step 4: Build + tests.**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL; tests PASS. If `ReminderSettingsMappingTest` constructs/round-trips `ReminderSettings`, it still passes (new field defaults true); add an assertion for the new field round-trip if the test is exhaustive.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(settings): period-logging reminder toggle (on by default)"
```

---

## Task 9: Period-reminder notifications (scheduler + receivers) (item 5)

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/reminders/NotificationChannels.kt`
- Modify: `app/src/main/java/com/domina/cycle/reminders/Notifier.kt`
- Modify: `app/src/main/java/com/domina/cycle/reminders/ReceiverEntryPoint.kt`
- Create: `app/src/main/java/com/domina/cycle/reminders/PeriodReminderScheduler.kt`
- Create: `app/src/main/java/com/domina/cycle/reminders/PeriodReminderReceiver.kt`
- Create: `app/src/main/java/com/domina/cycle/reminders/PeriodLogActionReceiver.kt`
- Modify: `app/src/main/java/com/domina/cycle/reminders/ReminderManager.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add the channel.** In `NotificationChannels.kt` add `const val CYCLE_LOG = "cycle_log"` and a Triple in `ensureCreated`:
`Triple(CYCLE_LOG, "Period logging reminders", NotificationManager.IMPORTANCE_DEFAULT),`.

- [ ] **Step 2: Expose the day-log repo to receivers.** In `ReceiverEntryPoint.kt` add `import com.domina.cycle.data.repository.DayLogRepository` and `fun dayLogRepository(): DayLogRepository`.

- [ ] **Step 3: Notifier methods.** In `Notifier.kt` add:
```kotlin
fun notifyPeriodLog(slot: Int) {
    NotificationChannels.ensureCreated(context)
    if (android.os.Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) return
    val notifId = PERIOD_NOTIF_BASE + slot
    val builder = NotificationCompat.Builder(context, NotificationChannels.CYCLE_LOG)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("How's your flow today?")
        .setContentText("Tap to log today's period 💛")
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    listOf("LIGHT" to "Light", "MEDIUM" to "Medium", "HEAVY" to "Heavy").forEachIndexed { i, (flow, label) ->
        val tap = Intent(context, PeriodLogActionReceiver::class.java).apply {
            putExtra(PeriodLogActionReceiver.EXTRA_FLOW, flow)
            putExtra(PeriodLogActionReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val pi = PendingIntent.getBroadcast(
            context, PERIOD_ACTION_BASE + slot * 10 + i, tap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(0, label, pi)
    }
    NotificationManagerCompat.from(context).notify(notifId, builder.build())
}

fun notifyPeriodConfirmation(notifId: Int, message: String) {
    NotificationChannels.ensureCreated(context)
    if (android.os.Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) return
    val n = NotificationCompat.Builder(context, NotificationChannels.CYCLE_LOG)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Logged 💛").setContentText(message)
        .setAutoCancel(true).setTimeoutAfter(8_000)
        .setPriority(NotificationCompat.PRIORITY_LOW).build()
    NotificationManagerCompat.from(context).notify(notifId, n)
}
```
Add to the companion: `private const val PERIOD_NOTIF_BASE = 7_100` and `private const val PERIOD_ACTION_BASE = 920_000`.

- [ ] **Step 4: Scheduler** — create `PeriodReminderScheduler.kt` (mirror of `CheckInScheduler`):
```kotlin
package com.domina.cycle.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Two exact alarms a day that prompt the user to log her period (see PeriodReminderReceiver). */
class PeriodReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService<AlarmManager>()!!

    fun scheduleAll() = SLOT_TIMES.indices.forEach { rescheduleSlot(it) }

    fun cancelAll() = SLOT_TIMES.indices.forEach { slot ->
        PendingIntent.getBroadcast(
            context, REQ_BASE + slot, Intent(context, PeriodReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { alarmManager.cancel(it) }
    }

    fun rescheduleSlot(slot: Int) {
        val now = LocalDateTime.now()
        var fire = LocalDateTime.of(LocalDate.now(), SLOT_TIMES[slot])
        if (!fire.isAfter(now)) fire = fire.plusDays(1)
        val triggerAt = fire.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, PeriodReminderReceiver::class.java).putExtra(EXTRA_SLOT, slot)
        val pi = PendingIntent.getBroadcast(
            context, REQ_BASE + slot, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    companion object {
        const val EXTRA_SLOT = "period_slot"
        private const val REQ_BASE = 930_000
        val SLOT_TIMES = listOf(LocalTime.of(10, 0), LocalTime.of(19, 0))
    }
}
```

- [ ] **Step 5: Fire receiver** — create `PeriodReminderReceiver.kt` (mirror `CheckInReceiver`, but checks mode + window):
```kotlin
package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.checkin.CheckInSchedule
import com.domina.cycle.domain.pregnancy.AppMode
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import com.domina.cycle.domain.reminders.PeriodLogPrompt
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class PeriodReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getIntExtra(PeriodReminderScheduler.EXTRA_SLOT, 0)
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
        val settings = ep.settings()
        val dayLogs = ep.dayLogRepository()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val enabled = settings.reminderSettings.first().periodLogReminders
                val mode = settings.appMode.first()
                val quiet = CheckInSchedule.isQuietHour(LocalTime.now().hour)
                if (enabled && mode != AppMode.PREGNANCY && !quiet) {
                    val today = LocalDate.now()
                    val logs = dayLogs.observeRange(today.minusDays(60), today).first()
                    val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
                    val pred = CyclePredictor.predict(periods, today)
                    if (PeriodLogPrompt.shouldPromptOn(today, logs, pred, periods)) {
                        Notifier(context).notifyPeriodLog(slot)
                    }
                }
                PeriodReminderScheduler(context).rescheduleSlot(slot)
            } finally {
                pending.finish()
            }
        }
    }
}
```

- [ ] **Step 6: Action receiver** — create `PeriodLogActionReceiver.kt` (mirror `CheckInActionReceiver`, writes flow):
```kotlin
package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class PeriodLogActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val flow = intent.getStringExtra(EXTRA_FLOW)
            ?.let { runCatching { FlowIntensity.valueOf(it) }.getOrNull() } ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
            .dayLogRepository()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val today = LocalDate.now()
                val existing = repo.getByDate(today) ?: DayLog(today)
                repo.save(existing.copy(flow = flow))
                Notifier(context).notifyPeriodConfirmation(notifId, "Logged ${flow.name.lowercase()} flow for today 💛")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_FLOW = "period_flow"
        const val EXTRA_NOTIF_ID = "period_notif_id"
    }
}
```

- [ ] **Step 7: Wire into `ReminderManager.reschedule`.** After the check-in scheduling line, add:
```kotlin
with(PeriodReminderScheduler(context)) {
    if (settings.periodLogReminders && mode != com.domina.cycle.domain.pregnancy.AppMode.PREGNANCY) scheduleAll() else cancelAll()
}
```
(`mode` is the value read in Task 3 Step 2.)

- [ ] **Step 8: Register receivers in the manifest.** In `AndroidManifest.xml`, next to the other `<receiver>` entries:
```xml
<receiver android:name=".reminders.PeriodReminderReceiver" android:exported="false" />
<receiver android:name=".reminders.PeriodLogActionReceiver" android:exported="false" />
```

- [ ] **Step 9: Build + full test run.**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL; all tests PASS.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat(reminders): twice-daily period-logging notifications with one-tap Light/Medium/Heavy"
```

---

## Final verification (on device — requires the phone unlocked)

- [ ] Switch to Pregnancy mode → Calendar shows no predicted period/fertile/ovulation; trimester tints + Due/Trimester list rows appear; Home shows no cycle cards.
- [ ] Log intimacy on a day → tiny ❤️ appears under that date.
- [ ] In cycle mode around the predicted period, confirm a period-log notification fires at 10:00 / 19:00 and tapping Light/Medium/Heavy writes flow without opening the app (verify via the day's log); confirm it stops once 7 days pass or the flow for the day is already logged.
- [ ] Settings → Reminders → toggle "Period logging reminders" off → no more period notifications after next reschedule.

## Self-review notes
- Spec items mapped: 1 & 3 → Tasks 1–3; 2 → Task 4; 4 → Tasks 5–6; 5 → Tasks 7–9. ✔
- `buildState` arg order: standardize as `buildState(logs, today, mode)` for Today and `buildState(m, logs, today, mode, dueDate)` for Calendar; ensure call sites match. ✔
- Type/name consistency: `CycleForecast.marksFor/outlookFor`, `PregnancyProjection.marksFor`, `PeriodLogPrompt.shouldPromptOn/windowFor`, `PeriodReminderScheduler.EXTRA_SLOT`, `PeriodLogActionReceiver.EXTRA_FLOW/EXTRA_NOTIF_ID`, `NotificationChannels.CYCLE_LOG`, `ReminderSettings.periodLogReminders`. ✔
