# Phase 7: UI/UX Polish — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Raise the visual quality across the app — cohesive warm theming (no more gray cards), readable system bars, a proper crescent-moon icon, a branded lock screen, a real weekday-aligned calendar with phase coloring, labeled charts, and a polished Today header.

**Architecture:** Pure UI/resource changes only — no data, security, or architecture changes. The highest-leverage change is giving each of the 3 themes a **complete Material 3 color scheme** (currently only 4 of ~25 slots are set, so Cards/containers fall back to default grays). Calendar grid layout becomes a pure, unit-tested helper.

**Tech Stack:** Jetpack Compose / Material 3, Compose Canvas, adaptive icons. No new dependencies. Still **no `INTERNET` permission**.

> Task 3 has a JVM unit test (calendar grid). The rest are visual; verify by build + install + launch (no crash). The controller will capture on-device screenshots for before/after. Avoid `connectedDebugAndroidTest` (wipes data) — use `installDebug`.

## Builds on (existing, on `main`)
- `ui/theme/{Color,Theme}.kt`, `MainActivity.kt`, `ui/lock/LockScreen.kt`, `ui/calendar/{CalendarViewModel,CalendarScreen}.kt`, `ui/insights/{Charts,InsightsScreen}.kt`, `ui/today/TodayScreen.kt`, `res/drawable/ic_launcher_foreground.xml`.
- `domain/prediction/{PeriodDeriver,CyclePredictor,CyclePhase}.kt`, `data/repository/DayLogRepository.kt`.

---

## Task 1: Complete warm color schemes + system-bar theming

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/theme/Theme.kt` (full schemes)
- Modify: `app/src/main/java/com/domina/cycle/MainActivity.kt` (status/nav bar appearance)

- [ ] **Step 1: Replace `Theme.kt` with complete per-theme `lightColorScheme`s**

```kotlin
package com.domina.cycle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.domina.cycle.data.prefs.ThemePreference

private val SoftSweet = lightColorScheme(
    primary = Color(0xFF8E5BBF), onPrimary = Color.White,
    primaryContainer = Color(0xFFEFE2FF), onPrimaryContainer = Color(0xFF2E0F4F),
    secondary = Color(0xFFC76B9A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0EE), onSecondaryContainer = Color(0xFF3E0023),
    tertiary = Color(0xFFE08A6B), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0D2), onTertiaryContainer = Color(0xFF3A1200),
    background = Color(0xFFFFF6FB), onBackground = Color(0xFF221721),
    surface = Color(0xFFFFF6FB), onSurface = Color(0xFF221721),
    surfaceVariant = Color(0xFFF3E3EF), onSurfaceVariant = Color(0xFF5B4B57),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFCEFF7),
    surfaceContainer = Color(0xFFF8E9F2), surfaceContainerHigh = Color(0xFFF3E3EF),
    surfaceContainerHighest = Color(0xFFEDDCE9),
    outline = Color(0xFF8C7A88), outlineVariant = Color(0xFFDEC9D8),
)

private val BrightJoyful = lightColorScheme(
    primary = Color(0xFF12A594), onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F2E8), onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFE63E5C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DF), onSecondaryContainer = Color(0xFF40000D),
    tertiary = Color(0xFFC79200), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8A3), onTertiaryContainer = Color(0xFF241A00),
    background = Color(0xFFFFF8F0), onBackground = Color(0xFF211B16),
    surface = Color(0xFFFFF8F0), onSurface = Color(0xFF211B16),
    surfaceVariant = Color(0xFFFCE7D6), onSurfaceVariant = Color(0xFF5C4633),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFF2E6),
    surfaceContainer = Color(0xFFFCEDDD), surfaceContainerHigh = Color(0xFFF8E6D2),
    surfaceContainerHighest = Color(0xFFF3DFC8),
    outline = Color(0xFF8C7259), outlineVariant = Color(0xFFE6CDB4),
)

private val WarmCozy = lightColorScheme(
    primary = Color(0xFF5E7A4F), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EBC8), onPrimaryContainer = Color(0xFF14210B),
    secondary = Color(0xFFA8704F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC4), onSecondaryContainer = Color(0xFF351300),
    tertiary = Color(0xFF9A7B2E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E6B5), onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFFBF6EC), onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFFBF6EC), onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFE9E4D2), onSurfaceVariant = Color(0xFF4B4739),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF6F1E6),
    surfaceContainer = Color(0xFFF1ECE0), surfaceContainerHigh = Color(0xFFEBE6D9),
    surfaceContainerHighest = Color(0xFFE5E0D3),
    outline = Color(0xFF7C7867), outlineVariant = Color(0xFFCEC8B5),
)

private fun schemeFor(theme: ThemePreference) = when (theme) {
    ThemePreference.SOFT_SWEET -> SoftSweet
    ThemePreference.BRIGHT_JOYFUL -> BrightJoyful
    ThemePreference.WARM_COZY -> WarmCozy
}

@Composable
fun AppTheme(theme: ThemePreference, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = schemeFor(theme), typography = AppTypography, content = content)
}
```
(`Color.kt`'s old `Soft*/Bright*/Cozy*` vals may now be unused — that's fine; leave or delete. Do not delete `Type.kt`/`AppTypography`.)

- [ ] **Step 2: Make the system bars readable in `MainActivity`**

All three themes are light, so the status/nav bar icons must be **dark**. In `MainActivity.onCreate`, before/after `setContent`, set edge-to-edge with light bar appearance:
```kotlin
// at top of onCreate, before super if using enableEdgeToEdge, else after setContent is fine:
androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
controller.isAppearanceLightStatusBars = true
controller.isAppearanceLightNavigationBars = true
@Suppress("DEPRECATION")
window.statusBarColor = android.graphics.Color.TRANSPARENT
```
Place this in `onCreate` after `super.onCreate(...)`. (Dark icons on the light cream/pink backgrounds become readable.)

- [ ] **Step 3: Build & install**

Run: `./gradlew :app:installDebug` → BUILD SUCCESSFUL; launch; `adb logcat -d` shows no crash. Cards (guidance, insights) are now warm-tinted, and the status-bar clock/icons are dark and readable.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/domina/cycle/ui/theme/Theme.kt app/src/main/java/com/domina/cycle/MainActivity.kt
git commit -m "feat(ui): complete warm color schemes + readable system bars"
```

---

## Task 2: Fix the launcher icon to a real crescent moon

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`

- [ ] **Step 1: Replace the foreground with a proper crescent moon (no star artifact)**

A crescent is one large filled circle with a smaller offset circle subtracted, drawn with an even-odd fill, centered in the adaptive safe zone (the 108-viewport center is 54,54; keep art within ~r36 of center). Plus one small twinkle star, clearly separated.
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <!-- crescent: outer circle minus an offset inner circle (evenOdd) -->
    <path android:fillColor="#FFFDF4FA" android:fillType="evenOdd"
        android:pathData="M58,30 m-22,0 a22,22 0,1 0,44 0 a22,22 0,1 0,-44 0 z
                          M66,26 m-20,0 a20,20 0,1 0,40 0 a20,20 0,1 0,-40 0 z"/>
    <!-- small twinkle -->
    <path android:fillColor="#FFFFE08A"
        android:pathData="M40,40 l1.6,4 4,0.3 -3,2.6 1,4 -3.6,-2.3 -3.6,2.3 1,-4 -3,-2.6 4,-0.3 z"/>
</vector>
```
> If the even-odd subtraction renders solid (no crescent), invert the two sub-paths' winding or move the inner circle further off-center. Verify visually in Step 2.

- [ ] **Step 2: Build, install, and visually verify the icon**

Run: `./gradlew :app:installDebug`. Then capture the launcher icon: `adb shell monkey -p com.domina.cycle 1 >/dev/null 2>&1` is not needed; instead confirm the drawable compiles and the app installs. The controller will screenshot the home screen to confirm the crescent renders.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/ic_launcher_foreground.xml
git commit -m "fix(ui): crescent-moon launcher icon (was rendering as a star)"
```

---

## Task 3: Real weekday-aligned calendar with phase coloring

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/calendar/CalendarMonth.kt` (pure)
- Modify: `app/src/main/java/com/domina/cycle/ui/calendar/CalendarViewModel.kt`, `CalendarScreen.kt`
- Test: `app/src/test/java/com/domina/cycle/ui/calendar/CalendarMonthTest.kt`

- [ ] **Step 1: Write the failing test for the pure grid builder**

```kotlin
package com.domina.cycle.ui.calendar

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.YearMonth

class CalendarMonthTest {
    @Test fun leadingBlanksAlignFirstDayToWeekdayMondayStart() {
        // May 2026: the 1st is a Friday -> Mon-start => 4 leading blanks
        val cells = CalendarMonth.cells(YearMonth.of(2026, 5))
        assertThat(cells.take(4)).containsExactly(null, null, null, null).inOrder()
        assertThat(cells[4]).isEqualTo(1)
        assertThat(cells.filterNotNull()).hasSize(31)
        assertThat(cells.size % 7).isEqualTo(0) // padded to full weeks
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*CalendarMonthTest*"`
Expected: FAIL — unresolved `CalendarMonth`.

- [ ] **Step 3: Implement `CalendarMonth`**

```kotlin
package com.domina.cycle.ui.calendar

import java.time.DayOfWeek
import java.time.YearMonth

/** Month grid as cells (null = blank), Monday-start, padded to whole weeks. */
object CalendarMonth {
    val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    fun cells(month: YearMonth): List<Int?> {
        val leading = (month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val days = (1..month.lengthOfMonth()).toList()
        val out = ArrayList<Int?>(leading + days.size)
        repeat(leading) { out.add(null) }
        out.addAll(days)
        while (out.size % 7 != 0) out.add(null)
        return out
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*CalendarMonthTest*"`
Expected: PASS.

- [ ] **Step 5: Expose day classification from `CalendarViewModel`**

Replace `CalendarViewModel` so it exposes, for the visible month: the cell list, a set of period days (flow logged), today, and the predicted next-period date. Keep `visibleMonth`, `nextMonth`, `prevMonth`.
```kotlin
package com.domina.cycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth,
    val cells: List<Int?>,
    val periodDays: Set<Int>,
    val today: Int?,           // day-of-month if today is in this month, else null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: DayLogRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = month.asStateFlow()

    val state: StateFlow<CalendarUiState> = month.flatMapLatest { m ->
        repository.observeRange(m.atDay(1), m.atEndOfMonth()).map { logs ->
            val periodDays = logs.filter { it.flow != FlowIntensity.NONE }.map { it.date.dayOfMonth }.toSet()
            val today = LocalDate.now().takeIf { YearMonth.from(it) == m }?.dayOfMonth
            CalendarUiState(m, CalendarMonth.cells(m), periodDays, today)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        CalendarUiState(YearMonth.now(), CalendarMonth.cells(YearMonth.now()), emptySet(), LocalDate.now().dayOfMonth))

    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun prevMonth() { month.value = month.value.minusMonths(1) }
}
```

- [ ] **Step 6: Rewrite `CalendarScreen` (headers, aligned grid, today ring, period color, legend)**

```kotlin
package com.domina.cycle.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(vm: CalendarViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::prevMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous") }
            Text(
                "${s.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${s.month.year}",
                Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = vm::nextMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            CalendarMonth.weekdayLabels.forEach { d ->
                Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7)) {
            items(s.cells) { day ->
                Box(Modifier.aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (day != null) {
                        val isPeriod = day in s.periodDays
                        val isToday = day == s.today
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (isPeriod) cs.secondaryContainer else cs.surfaceContainerHigh)
                                .then(if (isToday) Modifier.border(2.dp, cs.primary, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$day", style = MaterialTheme.typography.bodyMedium,
                                color = if (isPeriod) cs.onSecondaryContainer else cs.onSurface,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(cs.secondaryContainer))
            Text("  Period", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(14.dp).clip(CircleShape).border(2.dp, cs.primary, CircleShape))
            Text("  Today", style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

- [ ] **Step 7: Build, install, run full unit suite, commit**

Run: `./gradlew :app:testDebugUnitTest` (all pass incl. CalendarMonthTest) and `./gradlew :app:installDebug` (launch, no crash).
```bash
git add app/src/main/java/com/domina/cycle/ui/calendar/ app/src/test/java/com/domina/cycle/ui/calendar/CalendarMonthTest.kt
git commit -m "feat(ui): weekday-aligned calendar with period coloring, today ring, legend"
```

---

## Task 4: Redesign the lock screen

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/lock/LockScreen.kt`

- [ ] **Step 1: Rewrite `LockScreen` with branding + warmth**

A centered moon emoji + "Domina" wordmark, a friendly subtitle, a centered (not full-width) PIN field on a tinted surface, a filled primary button, and the biometric option as a subtle text button.
```kotlin
package com.domina.cycle.ui.lock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LockScreen(
    hasPin: Boolean,
    onPinEntered: (String) -> Unit,
    onUseBiometric: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🌙", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text("Domina", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasPin) "Welcome back 💛" else "Set a PIN to keep your data private 💛",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(0.7f),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onPinEntered(pin); pin = "" },
            enabled = pin.length >= 4,
            modifier = Modifier.fillMaxWidth(0.7f).height(52.dp),
        ) { Text(if (hasPin) "Unlock" else "Save PIN") }
        if (hasPin) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onUseBiometric) { Text("Use fingerprint / face") }
        }
    }
}
```

- [ ] **Step 2: Build, install, commit**

Run: `./gradlew :app:installDebug` → BUILD SUCCESSFUL, no crash on launch.
```bash
git add app/src/main/java/com/domina/cycle/ui/lock/LockScreen.kt
git commit -m "feat(ui): branded, warmer lock screen"
```

---

## Task 5: Label the Insights charts

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/insights/Charts.kt`

- [ ] **Step 1: Add value labels (bars) and min/max labels (lines)**

Update `BarChart` to draw each value above its bar, and `LineChart` to draw the min and max y-values at the left edge. Use `drawContext.canvas.nativeCanvas.drawText` with an `android.graphics.Paint`. Add to `BarChart` inside the Canvas after drawing each bar:
```kotlin
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY; textSize = 28f; textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        values.forEachIndexed { i, v ->
            val cx = pad + i * slot + slot / 2
            drawContext.canvas.nativeCanvas.drawText("$v", cx, h - pad - (h - 2 * pad) * (v.toFloat() / maxV) - 10f, labelPaint)
        }
```
And in `LineChart`, after computing `minV`/`maxV`, draw the two bounds:
```kotlin
        val axisPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY; textSize = 24f; isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(String.format("%.1f", maxV), 4f, y(maxV) + 8f, axisPaint)
        drawContext.canvas.nativeCanvas.drawText(String.format("%.1f", minV), 4f, y(minV) + 8f, axisPaint)
```
(Keep the existing line/bar drawing; these are additive. Ensure `drawContext.canvas.nativeCanvas` is accessible inside `Canvas { ... }` — it is in a `DrawScope`.)

- [ ] **Step 2: Build, install, commit**

Run: `./gradlew :app:installDebug` → BUILD SUCCESSFUL, no crash.
```bash
git add app/src/main/java/com/domina/cycle/ui/insights/Charts.kt
git commit -m "feat(ui): value & axis labels on insights charts"
```

---

## Task 6: Polish the Today header

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/today/TodayScreen.kt`

- [ ] **Step 1: Replace the generic "Hello 💛" with a time-aware greeting + date**

At the top of the `TodayScreen` column, replace the `Text("Hello 💛", ...)` with:
```kotlin
        val greeting = when (java.time.LocalTime.now().hour) {
            in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening"
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text("$greeting 💛", style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(
                state.today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
```
(Keep the rest of the screen; just swap the header. `state.today` already exists in the Today UI state.)

- [ ] **Step 2: Build, install, run full suites, commit**

Run: `./gradlew :app:testDebugUnitTest` (all pass) and `./gradlew :app:installDebug` (no crash).
```bash
git add app/src/main/java/com/domina/cycle/ui/today/TodayScreen.kt
git commit -m "feat(ui): time-aware greeting + date on Today"
```

---

## Phase 7 Definition of Done

- All 3 themes have complete Material 3 palettes — Cards/containers are warm-tinted (no more default gray); status-bar icons are dark/readable.
- The launcher icon is a crescent moon (not a star).
- The calendar is weekday-aligned with headers, period coloring, a today ring, and a legend.
- Insights charts show values/axis labels; the lock screen is branded; Today has a time-aware greeting + date.
- Full unit suite green (incl. CalendarMonthTest); app installs and launches with no crash. Still **no `INTERNET` permission**; no new dependencies.

## Deferred
- Per-day phase coloring across the whole calendar month; chart x-axis date labels; a designer-grade icon; custom typography/fonts.
```
