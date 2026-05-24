# Phase 4a: Pregnancy Mode — Week-by-Week Size Comparisons & Countdown — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user switch the app into Pregnancy mode (set a due date) and see a warm week-by-week pregnancy dashboard — "your baby is the size of a 🥑 avocado", with approximate measurements, a development blurb, a playful fun fact, current week/trimester, and a due-date countdown.

**Architecture:** A bundled, **pure-Kotlin 40-week dataset** (`PregnancyWeeks`) and a pure `PregnancyCalculator` (due date / current week / days remaining / trimester) — both fully unit-tested, offline, no assets I/O. App `mode` + due date persist in DataStore. The Today dashboard branches: Pregnancy mode shows the pregnancy content; otherwise the Phase-2 cycle ring.

**Tech Stack:** Kotlin, java.time, Jetpack Compose, Hilt, DataStore. Builds on Phases 1–3 (on `main`).

> Tasks 1–3 are pure-JVM unit-tested (no device). Task 4 is UI (needs the connected Galaxy S23 for install/launch verification). After any `connectedDebugAndroidTest`, run `./gradlew :app:installDebug` so the app stays on the phone.

## Builds on (existing, on `main`)
- `data/prefs/SettingsRepository.kt` — DataStore-backed (`theme`, `pinHash`, `reminderSettings`). **Extended here** with `appMode` + `dueDate`.
- `ui/today/TodayViewModel.kt` (`state: StateFlow<TodayUiState>`, `today`, pure `buildState`) + `TodayScreen.kt` (cycle ring). **Extended here** to branch on mode.
- `domain/` package convention for pure logic.

## New file structure
```
app/src/main/java/com/domina/cycle/
  domain/pregnancy/
    AppMode.kt              # enum CYCLE / TTC / PREGNANCY
    PregnancyWeek.kt        # data class for one week's content
    PregnancyWeeks.kt       # the bundled 40-week dataset + forWeek()
    PregnancyProgress.kt    # data class: week, dayInWeek, daysRemaining, trimester, dueDate
    PregnancyCalculator.kt  # pure: dueDate/week/progress math
  data/prefs/SettingsRepository.kt   # MODIFY: appMode + dueDate
  ui/today/TodayViewModel.kt         # MODIFY: expose mode + pregnancy progress/week
  ui/today/PregnancyDashboard.kt     # NEW: pregnancy content composable
  ui/today/TodayScreen.kt            # MODIFY: branch cycle vs pregnancy
  ui/settings/SettingsViewModel.kt   # MODIFY: mode + due date setters
  ui/settings/SettingsScreen.kt      # MODIFY: mode switch + due-date entry
app/src/test/java/com/domina/cycle/domain/pregnancy/
  PregnancyWeeksTest.kt, PregnancyCalculatorTest.kt
app/src/test/java/com/domina/cycle/data/prefs/PregnancyPrefsMappingTest.kt
```

---

## Task 1: Pregnancy week dataset

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyWeek.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyWeeks.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/pregnancy/PregnancyWeeksTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.pregnancy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PregnancyWeeksTest {
    @Test fun coversWeeks4Through40Contiguously() {
        val weeks = PregnancyWeeks.all.map { it.week }
        assertThat(weeks).isEqualTo((4..40).toList())
    }

    @Test fun everyWeekHasContent() {
        PregnancyWeeks.all.forEach { w ->
            assertThat(w.fruit).isNotEmpty()
            assertThat(w.emoji).isNotEmpty()
            assertThat(w.development).isNotEmpty()
            assertThat(w.funFact).isNotEmpty()
            assertThat(w.lengthCm).isAtLeast(0.0)
        }
    }

    @Test fun forWeekReturnsExactMatch() {
        assertThat(PregnancyWeeks.forWeek(16).fruit).contains("avocado")
    }

    @Test fun forWeekClampsOutOfRange() {
        assertThat(PregnancyWeeks.forWeek(1).week).isEqualTo(4)   // below range -> first
        assertThat(PregnancyWeeks.forWeek(99).week).isEqualTo(40) // above range -> last
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PregnancyWeeksTest*"`
Expected: FAIL — unresolved `PregnancyWeeks` / `PregnancyWeek`.

- [ ] **Step 3: Implement `PregnancyWeek` and `PregnancyWeeks`**

`PregnancyWeek.kt`:
```kotlin
package com.domina.cycle.domain.pregnancy

data class PregnancyWeek(
    val week: Int,
    val fruit: String,
    val emoji: String,
    val lengthCm: Double,
    val weightG: Int,
    val development: String,
    val funFact: String,
)
```
`PregnancyWeeks.kt` (bundled, offline; approximate sizes — playful, not medical):
```kotlin
package com.domina.cycle.domain.pregnancy

/** Bundled week-by-week pregnancy content. Sizes are friendly approximations, not medical data. */
object PregnancyWeeks {
    val all: List<PregnancyWeek> = listOf(
        PregnancyWeek(4, "poppy seed", "⚫", 0.1, 0, "The neural tube is forming.", "Baby is just a tiny dot — but a busy one!"),
        PregnancyWeek(5, "sesame seed", "⚫", 0.2, 0, "The heart begins to beat.", "That first heartbeat starts around now 💛"),
        PregnancyWeek(6, "lentil", "🟢", 0.5, 0, "Tiny limb buds appear.", "Little arm and leg buds are sprouting."),
        PregnancyWeek(7, "blueberry", "🫐", 1.0, 1, "The brain is developing fast.", "Baby's head is growing quickly this week."),
        PregnancyWeek(8, "raspberry", "🍓", 1.6, 1, "Fingers and toes are forming.", "Webbed little fingers are taking shape."),
        PregnancyWeek(9, "cherry", "🍒", 2.3, 2, "Tiny earlobes appear.", "Baby now has the beginnings of ears."),
        PregnancyWeek(10, "strawberry", "🍓", 3.1, 4, "Vital organs are working.", "Tiny nails are starting to form."),
        PregnancyWeek(11, "lime", "🍋", 4.1, 7, "Baby can hiccup.", "Those first practice hiccups begin."),
        PregnancyWeek(12, "plum", "🟣", 5.4, 14, "Reflexes are developing.", "Baby can curl tiny fingers and toes."),
        PregnancyWeek(13, "lemon", "🍋", 7.4, 23, "Fingerprints are forming.", "Unique little fingerprints appear this week."),
        PregnancyWeek(14, "peach", "🍑", 8.7, 43, "Baby can squint and frown.", "Tiny facial expressions are starting!"),
        PregnancyWeek(15, "apple", "🍎", 10.1, 70, "Sensing light through eyelids.", "Baby may sense bright light now."),
        PregnancyWeek(16, "avocado", "🥑", 11.6, 100, "Tiny ears are tuning in.", "Baby is starting to hear your voice 💛"),
        PregnancyWeek(17, "pear", "🍐", 13.0, 140, "The skeleton is hardening.", "Soft cartilage is turning to bone."),
        PregnancyWeek(18, "bell pepper", "🫑", 14.2, 190, "Yawning and stretching.", "Baby is wriggling around in there."),
        PregnancyWeek(19, "mango", "🥭", 15.3, 240, "Vernix coats the skin.", "A creamy protective layer forms."),
        PregnancyWeek(20, "banana", "🍌", 16.4, 300, "Halfway there — and can hear you!", "You're halfway! Sing away 🎵"),
        PregnancyWeek(21, "carrot", "🥕", 26.7, 360, "Eyebrows are forming.", "Baby is practicing little movements."),
        PregnancyWeek(22, "spaghetti squash", "🌟", 27.8, 430, "Sense of touch develops.", "Baby may grab the umbilical cord."),
        PregnancyWeek(23, "grapefruit", "🍊", 28.9, 501, "Hearing is improving.", "Loud sounds may make baby move."),
        PregnancyWeek(24, "ear of corn", "🌽", 30.0, 600, "The face is fully formed.", "Baby has eyelashes now!"),
        PregnancyWeek(25, "rutabaga", "🌟", 34.6, 660, "Responding to your voice.", "Baby may react when you talk 💛"),
        PregnancyWeek(26, "scallion", "🌱", 35.6, 760, "Eyes will open soon.", "Baby is taking practice breaths."),
        PregnancyWeek(27, "cauliflower", "🌿", 36.6, 875, "Sleep and wake cycles begin.", "Baby is settling into a rhythm."),
        PregnancyWeek(28, "eggplant", "🍆", 37.6, 1005, "Dreaming (REM) begins.", "Third trimester starts — you've got this!"),
        PregnancyWeek(29, "butternut squash", "🌟", 38.6, 1153, "Muscles are maturing.", "Those kicks are getting stronger."),
        PregnancyWeek(30, "cabbage", "🥬", 39.9, 1319, "Eyes can track light.", "Baby's brain is growing fast."),
        PregnancyWeek(31, "coconut", "🥥", 41.1, 1502, "All five senses are working.", "Baby can turn their head now."),
        PregnancyWeek(32, "napa cabbage", "🥬", 42.4, 1702, "Practicing breathing.", "Fingernails reach the fingertips."),
        PregnancyWeek(33, "pineapple", "🍍", 43.7, 1918, "Bones are hardening.", "The skull stays soft and flexible for birth."),
        PregnancyWeek(34, "cantaloupe", "🍈", 45.0, 2146, "May recognize songs.", "Play a favorite tune — baby's listening!"),
        PregnancyWeek(35, "honeydew", "🍈", 46.2, 2383, "Kidneys are fully developed.", "Baby is plumping up nicely."),
        PregnancyWeek(36, "romaine lettuce", "🥬", 47.4, 2622, "Getting into position.", "Baby may be settling head-down."),
        PregnancyWeek(37, "swiss chard", "🌿", 48.6, 2859, "Considered early term soon.", "Baby is practicing breathing and sucking."),
        PregnancyWeek(38, "leek", "🌱", 49.8, 3083, "A firm little grasp.", "Baby's grip is surprisingly strong!"),
        PregnancyWeek(39, "mini watermelon", "🍉", 50.7, 3288, "Ready any day now.", "Fully developed and getting cozy."),
        PregnancyWeek(40, "small pumpkin", "🎃", 51.2, 3462, "Welcome, baby! 💛", "Only about 5% of babies arrive on their due date."),
    )

    fun forWeek(week: Int): PregnancyWeek {
        val clamped = week.coerceIn(all.first().week, all.last().week)
        return all.first { it.week == clamped }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PregnancyWeeksTest*"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyWeek.kt \
        app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyWeeks.kt \
        app/src/test/java/com/domina/cycle/domain/pregnancy/PregnancyWeeksTest.kt
git commit -m "feat: bundled 40-week pregnancy size-comparison dataset"
```

---

## Task 2: PregnancyCalculator (due date / week / progress)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyProgress.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyCalculator.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/pregnancy/PregnancyCalculatorTest.kt`

Contract: a full term is 280 days (40 weeks) from LMP; `dueDate = lmp + 280d`. Gestational age in days on `today` = `280 - (dueDate - today)`. `week = gestationalDays / 7 + 1`? Use the obstetric convention where the count is completed weeks + 1 day-of-week: define `completedDays = 280 - daysUntilDue` (clamped ≥0), `week = completedDays / 7` (0-based weeks completed) reported as `weeksCompleted`, and `dayInWeek = completedDays % 7`. Display week = `weeksCompleted`. Trimester: 1 (weeks 1–13), 2 (14–27), 3 (28+). `daysRemaining = dueDate - today` (can be negative if overdue).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.pregnancy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PregnancyCalculatorTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun dueDateIs280DaysAfterLmp() {
        assertThat(PregnancyCalculator.dueDateFromLmp(d("2026-01-01"))).isEqualTo(d("2026-10-08"))
    }

    @Test fun progressAtConfirmedDueDate() {
        val due = d("2026-10-08")
        val p = PregnancyCalculator.progress(due, today = due)
        assertThat(p.weeksCompleted).isEqualTo(40)
        assertThat(p.daysRemaining).isEqualTo(0)
        assertThat(p.trimester).isEqualTo(3)
    }

    @Test fun progressMidPregnancy() {
        val due = d("2026-10-08")        // lmp 2026-01-01
        val p = PregnancyCalculator.progress(due, today = d("2026-04-23")) // 112 days after lmp = week 16
        assertThat(p.weeksCompleted).isEqualTo(16)
        assertThat(p.dayInWeek).isEqualTo(0)
        assertThat(p.trimester).isEqualTo(2)
        assertThat(p.daysRemaining).isEqualTo(168)
    }

    @Test fun overdueGivesNegativeDaysRemaining() {
        val due = d("2026-10-08")
        val p = PregnancyCalculator.progress(due, today = d("2026-10-11"))
        assertThat(p.daysRemaining).isEqualTo(-3)
        assertThat(p.weeksCompleted).isAtLeast(40)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PregnancyCalculatorTest*"`
Expected: FAIL — unresolved `PregnancyCalculator` / `PregnancyProgress`.

- [ ] **Step 3: Implement `PregnancyProgress` and `PregnancyCalculator`**

`PregnancyProgress.kt`:
```kotlin
package com.domina.cycle.domain.pregnancy

import java.time.LocalDate

data class PregnancyProgress(
    val dueDate: LocalDate,
    val weeksCompleted: Int,
    val dayInWeek: Int,
    val daysRemaining: Int,
    val trimester: Int,
)
```
`PregnancyCalculator.kt`:
```kotlin
package com.domina.cycle.domain.pregnancy

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object PregnancyCalculator {
    private const val TERM_DAYS = 280L // 40 weeks

    fun dueDateFromLmp(lmp: LocalDate): LocalDate = lmp.plusDays(TERM_DAYS)

    fun progress(dueDate: LocalDate, today: LocalDate): PregnancyProgress {
        val daysRemaining = ChronoUnit.DAYS.between(today, dueDate).toInt()
        val completedDays = (TERM_DAYS - daysRemaining).toInt().coerceAtLeast(0)
        val weeksCompleted = completedDays / 7
        val dayInWeek = completedDays % 7
        val trimester = when {
            weeksCompleted <= 13 -> 1
            weeksCompleted <= 27 -> 2
            else -> 3
        }
        return PregnancyProgress(dueDate, weeksCompleted, dayInWeek, daysRemaining, trimester)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PregnancyCalculatorTest*"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyProgress.kt \
        app/src/main/java/com/domina/cycle/domain/pregnancy/PregnancyCalculator.kt \
        app/src/test/java/com/domina/cycle/domain/pregnancy/PregnancyCalculatorTest.kt
git commit -m "feat: pregnancy due-date/week/trimester calculator"
```

---

## Task 3: App mode + due date persistence

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/pregnancy/AppMode.kt`
- Modify: `app/src/main/java/com/domina/cycle/data/prefs/SettingsRepository.kt`
- Test: `app/src/test/java/com/domina/cycle/data/prefs/PregnancyPrefsMappingTest.kt`

- [ ] **Step 1: Write the failing test (pure mapping)**

```kotlin
package com.domina.cycle.data.prefs

import com.domina.cycle.domain.pregnancy.AppMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PregnancyPrefsMappingTest {
    @Test fun appModeRoundTripsByName() {
        AppMode.entries.forEach { assertThat(AppMode.fromName(it.name)).isEqualTo(it) }
    }
    @Test fun unknownModeFallsBackToCycle() {
        assertThat(AppMode.fromName("nonsense")).isEqualTo(AppMode.CYCLE)
        assertThat(AppMode.fromName(null)).isEqualTo(AppMode.CYCLE)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PregnancyPrefsMappingTest*"`
Expected: FAIL — unresolved `AppMode`.

- [ ] **Step 3: Implement `AppMode` and extend `SettingsRepository`**

`AppMode.kt`:
```kotlin
package com.domina.cycle.domain.pregnancy

enum class AppMode { CYCLE, TTC, PREGNANCY;
    companion object {
        fun fromName(name: String?): AppMode = entries.firstOrNull { it.name == name } ?: CYCLE
    }
}
```
Append to `SettingsRepository` (keep existing code; reuse the existing `dataStore`):
```kotlin
    // --- app mode + pregnancy ---
    private val appModeKey = stringPreferencesKey("app_mode")
    private val dueDateKey = androidx.datastore.preferences.core.longPreferencesKey("due_date_epoch_day")

    val appMode: Flow<com.domina.cycle.domain.pregnancy.AppMode> =
        context.dataStore.data.map { com.domina.cycle.domain.pregnancy.AppMode.fromName(it[appModeKey]) }

    val dueDate: Flow<java.time.LocalDate?> =
        context.dataStore.data.map { p -> p[dueDateKey]?.let { java.time.LocalDate.ofEpochDay(it) } }

    suspend fun setAppMode(mode: com.domina.cycle.domain.pregnancy.AppMode) {
        context.dataStore.edit { it[appModeKey] = mode.name }
    }

    suspend fun setDueDate(date: java.time.LocalDate?) {
        context.dataStore.edit {
            if (date == null) it.remove(dueDateKey) else it[dueDateKey] = date.toEpochDay()
        }
    }
```
Ensure `stringPreferencesKey` is imported (it already is from Phase 1/3).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PregnancyPrefsMappingTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/pregnancy/AppMode.kt \
        app/src/main/java/com/domina/cycle/data/prefs/SettingsRepository.kt \
        app/src/test/java/com/domina/cycle/data/prefs/PregnancyPrefsMappingTest.kt
git commit -m "feat: persist app mode and pregnancy due date"
```

---

## Task 4: Pregnancy dashboard + mode switch UI

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/today/TodayViewModel.kt`
- Create: `app/src/main/java/com/domina/cycle/ui/today/PregnancyDashboard.kt`
- Modify: `app/src/main/java/com/domina/cycle/ui/today/TodayScreen.kt`
- Modify: `app/src/main/java/com/domina/cycle/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/domina/cycle/ui/settings/SettingsScreen.kt`
- Test: `app/src/test/java/com/domina/cycle/ui/today/PregnancyModeStateTest.kt`

- [ ] **Step 1: Write the failing test for the ViewModel's pregnancy state builder**

```kotlin
package com.domina.cycle.ui.today

import com.domina.cycle.domain.pregnancy.AppMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class PregnancyModeStateTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun pregnancyModeWithDueDateProducesProgressAndWeekContent() {
        val s = TodayViewModel.buildPregnancyState(
            mode = AppMode.PREGNANCY, dueDate = d("2026-10-08"), today = d("2026-04-23"))
        assertThat(s).isNotNull()
        assertThat(s!!.progress.weeksCompleted).isEqualTo(16)
        assertThat(s.week.fruit).contains("avocado")
    }

    @Test fun pregnancyModeWithoutDueDateIsNull() {
        assertThat(
            TodayViewModel.buildPregnancyState(AppMode.PREGNANCY, dueDate = null, today = d("2026-04-23"))
        ).isNull()
    }

    @Test fun nonPregnancyModeIsNull() {
        assertThat(
            TodayViewModel.buildPregnancyState(AppMode.CYCLE, dueDate = d("2026-10-08"), today = d("2026-04-23"))
        ).isNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PregnancyModeStateTest*"`
Expected: FAIL — `buildPregnancyState` / `PregnancyUiState` not found.

- [ ] **Step 3: Extend `TodayViewModel` with pregnancy state**

Add to `TodayViewModel.kt` (keep existing cycle `state`; add a parallel `pregnancy` flow and a pure builder). Add these imports: `com.domina.cycle.data.prefs.SettingsRepository`, `com.domina.cycle.domain.pregnancy.*`, `kotlinx.coroutines.flow.combine`. Change the constructor to also inject `SettingsRepository`:
```kotlin
data class PregnancyUiState(
    val progress: PregnancyProgress,
    val week: PregnancyWeek,
)
```
Inside `TodayViewModel` (constructor becomes `repository: DayLogRepository, settings: SettingsRepository`):
```kotlin
    val pregnancy: StateFlow<PregnancyUiState?> =
        combine(settings.appMode, settings.dueDate) { mode, due ->
            buildPregnancyState(mode, due, LocalDate.now())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```
And in the `companion object`:
```kotlin
        fun buildPregnancyState(mode: AppMode, dueDate: LocalDate?, today: LocalDate): PregnancyUiState? {
            if (mode != AppMode.PREGNANCY || dueDate == null) return null
            val progress = PregnancyCalculator.progress(dueDate, today)
            val week = PregnancyWeeks.forWeek(progress.weeksCompleted)
            return PregnancyUiState(progress, week)
        }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PregnancyModeStateTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Implement `PregnancyDashboard` composable**

`PregnancyDashboard.kt`:
```kotlin
package com.domina.cycle.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PregnancyDashboard(state: PregnancyUiState, modifier: Modifier = Modifier) {
    val w = state.week
    val p = state.progress
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Week ${p.weeksCompleted} · Trimester ${p.trimester}", style = MaterialTheme.typography.labelLarge)
        Text(w.emoji, fontSize = 72.sp)
        Text("Size of a ${w.fruit}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (w.lengthCm > 0) {
            Text("~${w.lengthCm} cm" + if (w.weightG > 0) " · ${w.weightG} g" else "",
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(w.development, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("💛 ${w.funFact}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        val countdown = when {
            p.daysRemaining > 0 -> "${p.daysRemaining} days to go 💛"
            p.daysRemaining == 0 -> "Due today! 💛"
            else -> "${-p.daysRemaining} days past due — any moment now 💛"
        }
        AssistChip(onClick = {}, label = { Text(countdown) })
    }
}
```

- [ ] **Step 6: Branch `TodayScreen` on mode**

In `TodayScreen.kt`, collect the new pregnancy state and render it instead of the cycle ring when present. Add near the top of the composable (after getting `vm`):
```kotlin
    val pregnancy by vm.pregnancy.collectAsStateWithLifecycle()
```
Then wrap the existing cycle content: if `pregnancy != null`, show `PregnancyDashboard(pregnancy!!)` (plus the greeting and the "Log today" button); otherwise show the existing cycle ring + chips + guidance. Keep the `onLogToday` button visible in both. Minimal structure:
```kotlin
        Text("Hello 💛", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        val preg = pregnancy
        if (preg != null) {
            PregnancyDashboard(preg)
        } else {
            // ... existing cycle ring + confidence chips + guidance card ...
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onLogToday) { Text(if (state.todayLog == null) "Log today" else "Edit today's log") }
```

- [ ] **Step 7: Add mode switch + due-date entry to Settings**

In `SettingsViewModel.kt` add (inject already-present `settings`):
```kotlin
    val mode: StateFlow<com.domina.cycle.domain.pregnancy.AppMode> =
        settings.appMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.domina.cycle.domain.pregnancy.AppMode.CYCLE)
    val dueDate: StateFlow<java.time.LocalDate?> =
        settings.dueDate.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setMode(m: com.domina.cycle.domain.pregnancy.AppMode) { viewModelScope.launch { settings.setAppMode(m) } }
    fun setDueDate(date: java.time.LocalDate?) { viewModelScope.launch { settings.setDueDate(date) } }
```
In `SettingsScreen.kt` add a "Mode" section: a row of three `FilterChip`s (Cycle / Trying to conceive / Pregnancy) bound to `vm.setMode`, and — when mode is PREGNANCY — an `OutlinedTextField` to enter the due date as `YYYY-MM-DD`, parsed with `runCatching { LocalDate.parse(it) }`, calling `vm.setDueDate(parsed)` on a valid value (show the current due date as the initial text). Example block:
```kotlin
        Spacer(Modifier.height(16.dp))
        Text("Mode", style = MaterialTheme.typography.titleMedium)
        val mode by vm.mode.collectAsStateWithLifecycle()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.domina.cycle.domain.pregnancy.AppMode.entries.forEach { m ->
                FilterChip(selected = mode == m, onClick = { vm.setMode(m) },
                    label = { Text(when (m) {
                        com.domina.cycle.domain.pregnancy.AppMode.CYCLE -> "Cycle"
                        com.domina.cycle.domain.pregnancy.AppMode.TTC -> "Trying"
                        com.domina.cycle.domain.pregnancy.AppMode.PREGNANCY -> "Pregnancy" } )})
            }
        }
        if (mode == com.domina.cycle.domain.pregnancy.AppMode.PREGNANCY) {
            val due by vm.dueDate.collectAsStateWithLifecycle()
            var text by remember(due) { mutableStateOf(due?.toString() ?: "") }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; runCatching { java.time.LocalDate.parse(it) }.getOrNull()?.let(vm::setDueDate) },
                label = { Text("Due date (YYYY-MM-DD)") },
                singleLine = true,
            )
        }
```
Add imports for `remember`, `mutableStateOf`, `getValue`, `setValue` in `SettingsScreen.kt`.

- [ ] **Step 8: Build, run full unit suite, install, and verify on device**

Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
```
On device: open app → unlock → Settings → set Mode to **Pregnancy** → enter a due date a few months out (e.g., today + ~170 days) → go to the Today tab → confirm the pregnancy dashboard shows the week, the fruit emoji + "Size of a …", measurements, development + fun fact, and the countdown chip. Switch Mode back to **Cycle** → Today shows the cycle ring again. Check `adb logcat -d` for no crash.

- [ ] **Step 9: Reinstall (in case of any connected-test run) and commit**

```bash
./gradlew :app:installDebug
git add app/src/main/java/com/domina/cycle/ui/today/TodayViewModel.kt \
        app/src/main/java/com/domina/cycle/ui/today/PregnancyDashboard.kt \
        app/src/main/java/com/domina/cycle/ui/today/TodayScreen.kt \
        app/src/main/java/com/domina/cycle/ui/settings/SettingsViewModel.kt \
        app/src/main/java/com/domina/cycle/ui/settings/SettingsScreen.kt \
        app/src/test/java/com/domina/cycle/ui/today/PregnancyModeStateTest.kt
git commit -m "feat: pregnancy mode dashboard (week size comparison + countdown) and mode switch"
```

---

## Phase 4a Definition of Done

- 40-week dataset + pregnancy calculator + mode/due-date persistence are pure Kotlin with passing unit tests.
- Full unit suite green; Phase 1 instrumented suite still green; app installed on the device at the end.
- Switching to Pregnancy mode + setting a due date shows the week-by-week dashboard (size comparison, development, fun fact, countdown); switching back shows the cycle ring.
- Still **no `INTERNET` permission**.

## Deferred to Phase 4b
- Kick counter, contraction timer, pregnancy weight tracking, hospital-bag & birth-plan checklists (each its own small feature reusing this mode + a few new entities).
- A nicer due-date picker (Phase 4a uses a validated text field) and the option to set the due date from LMP.
- TTC mode currently behaves like Cycle mode on the dashboard; TTC-specific emphasis (fertile-window focus, BBT charting) is a later enhancement.
```
