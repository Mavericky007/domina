# Phase 6d: Home-Screen Widget — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A small home-screen widget that shows, at a glance, the current cycle day & phase with days-until-period (or, in pregnancy mode, the week & countdown) — tapping it opens the app.

**Architecture:** A pure-Kotlin `WidgetData.build(...)` turns logs + mode + due date into two short strings (headline + subline) — JVM-tested, reusing the existing predictor/pregnancy logic. A **Glance** `GlanceAppWidget` reads data via a Hilt `EntryPoint` (widgets aren't `@AndroidEntryPoint`), calls `WidgetData.build`, and renders it; a `GlanceAppWidgetReceiver` + provider XML register it. Still **no `INTERNET` permission**.

**Tech Stack:** Kotlin, **androidx.glance:glance-appwidget** (new dependency), Hilt EntryPoint, Compose-style Glance UI. Builds on Phases 1–6c (on `main`).

> Task 1 is pure-JVM unit-tested. Task 2 adds the Glance widget (compile + install + provider-registered verification; adding it to the home screen is manual). Glance APIs evolve — if the version/API in this plan doesn't compile, check the official Glance docs and adjust minimally, then report what changed.

## Builds on (existing, on `main`)
- `domain/prediction/{PeriodDeriver,CyclePredictor}.kt`, `domain/pregnancy/{AppMode,PregnancyCalculator,PregnancyWeeks}.kt`.
- `data/repository/DayLogRepository.kt`, `data/prefs/SettingsRepository.kt` (`appMode`, `dueDate`).
- `CycleApp` (`@HiltAndroidApp`), `MainActivity`.

## New file structure
```
app/src/main/java/com/domina/cycle/widget/
  WidgetData.kt            # pure: build headline/subline
  CycleWidget.kt           # GlanceAppWidget (reads via Hilt EntryPoint, renders)
  CycleWidgetReceiver.kt   # GlanceAppWidgetReceiver
app/src/main/res/xml/cycle_widget_info.xml   # appwidget-provider metadata
app/src/main/AndroidManifest.xml             # MODIFY: register receiver
gradle/libs.versions.toml, app/build.gradle.kts   # MODIFY: glance dependency
app/src/test/java/com/domina/cycle/widget/WidgetDataTest.kt
```

---

## Task 1: WidgetData (pure)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/widget/WidgetData.kt`
- Test: `app/src/test/java/com/domina/cycle/widget/WidgetDataTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.widget

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.pregnancy.AppMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class WidgetDataTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun flow(s: String) = DayLog(date = d(s), flow = FlowIntensity.MEDIUM)

    @Test fun noDataShowsWelcome() {
        val w = WidgetData.build(emptyList(), AppMode.CYCLE, null, d("2026-05-10"))
        assertThat(w.headline).isEqualTo("Domina")
        assertThat(w.subline).contains("Log")
    }

    @Test fun cycleModeShowsDayAndPhase() {
        val logs = listOf(flow("2026-02-01"), flow("2026-03-01"), flow("2026-03-29"), flow("2026-04-26"))
        val w = WidgetData.build(logs, AppMode.CYCLE, null, d("2026-05-05")) // cycle day 10
        assertThat(w.headline).isEqualTo("Cycle Day 10")
        assertThat(w.subline.lowercase()).contains("follicular")
    }

    @Test fun pregnancyModeShowsWeekAndCountdown() {
        val w = WidgetData.build(emptyList(), AppMode.PREGNANCY, d("2026-10-08"), d("2026-04-23"))
        assertThat(w.headline).isEqualTo("Week 16")
        assertThat(w.subline).contains("to go")
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*WidgetDataTest*"`
Expected: FAIL — unresolved `WidgetData`.

- [ ] **Step 3: Implement `WidgetData`**

```kotlin
package com.domina.cycle.widget

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.pregnancy.AppMode
import com.domina.cycle.domain.pregnancy.PregnancyCalculator
import com.domina.cycle.domain.prediction.Confidence
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WidgetData(val headline: String, val subline: String)

object WidgetData {
    fun build(logs: List<DayLog>, mode: AppMode, dueDate: LocalDate?, today: LocalDate): WidgetData {
        if (mode == AppMode.PREGNANCY && dueDate != null) {
            val p = PregnancyCalculator.progress(dueDate, today)
            val sub = if (p.daysRemaining >= 0) "${p.daysRemaining} days to go 💛" else "due any moment 💛"
            return WidgetData("Week ${p.weeksCompleted}", sub)
        }
        val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
        val pred = CyclePredictor.predict(periods, today)
        if (pred.confidence == Confidence.NONE || pred.cycleDay == null) {
            return WidgetData("Domina", "Log a period to begin 💛")
        }
        val phase = pred.phase?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
        val days = pred.nextPeriodDate?.let { ChronoUnit.DAYS.between(today, it).toInt() }
        val sub = when {
            days == null -> phase
            days <= 0 -> "$phase · period due"
            else -> "$phase · period in ${days}d"
        }
        return WidgetData("Cycle Day ${pred.cycleDay}", sub)
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*WidgetDataTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/widget/WidgetData.kt \
        app/src/test/java/com/domina/cycle/widget/WidgetDataTest.kt
git commit -m "feat: pure widget data builder"
```

---

## Task 2: Glance widget + receiver + registration

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (Glance dependency)
- Create: `widget/CycleWidget.kt`, `widget/CycleWidgetReceiver.kt`, `res/xml/cycle_widget_info.xml`
- Modify: `AndroidManifest.xml`

- [ ] **Step 1: Add the Glance dependency**

In `gradle/libs.versions.toml` `[versions]`: `glance = "1.1.1"`. In `[libraries]`:
```toml
glance-appwidget = { module = "androidx.glance:glance-appwidget", version.ref = "glance" }
glance-material3 = { module = "androidx.glance:glance-material3", version.ref = "glance" }
```
In `app/build.gradle.kts` dependencies: `implementation(libs.glance.appwidget)` and `implementation(libs.glance.material3)`.
> If `1.1.1` doesn't resolve, try `1.1.0`. Glance requires the Compose compiler plugin (already applied).

- [ ] **Step 2: Implement `CycleWidget` (reads data via Hilt EntryPoint, renders with Glance)**

```kotlin
package com.domina.cycle.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.domina.cycle.MainActivity
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun dayLogRepository(): DayLogRepository
    fun settingsRepository(): SettingsRepository
}

class CycleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val today = LocalDate.now()
        val logs = ep.dayLogRepository().observeRange(today.minusDays(400), today).first()
        val mode = ep.settingsRepository().appMode.first()
        val due = ep.settingsRepository().dueDate.first()
        val data = WidgetData.build(logs, mode, due, today)
        provideContent { WidgetContent(data) }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(data: WidgetData) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp)
                .background(ColorProvider(Color(0xFFFDF4FA)))
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            Text(data.headline, style = TextStyle(fontWeight = FontWeight.Bold,
                color = ColorProvider(Color(0xFF6A4C93))))
            Text(data.subline, style = TextStyle(color = ColorProvider(Color(0xFF8A5A9E))))
        }
    }
}
```

- [ ] **Step 3: Implement `CycleWidgetReceiver`**

```kotlin
package com.domina.cycle.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class CycleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CycleWidget()
}
```

- [ ] **Step 4: Add the provider metadata + register in the manifest**

`app/src/main/res/xml/cycle_widget_info.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp"
    android:minHeight="40dp"
    android:targetCellWidth="2"
    android:targetCellHeight="1"
    android:updatePeriodMillis="86400000"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:initialLayout="@layout/glance_default_loading_layout" />
```
> `@layout/glance_default_loading_layout` is provided by the glance-appwidget library. If the build can't find it, create a minimal `res/layout/widget_loading.xml` (a `FrameLayout`) and reference that instead.

In `AndroidManifest.xml` `<application>`:
```xml
        <receiver android:name=".widget.CycleWidgetReceiver" android:exported="false">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider"
                android:resource="@xml/cycle_widget_info" />
        </receiver>
```

- [ ] **Step 5: (Optional) refresh the widget from the daily worker**

If straightforward, in `DailyRecomputeWorker.doWork()` after rescheduling, add `CycleWidget().updateAll(applicationContext)` so the widget refreshes daily. If this complicates the worker, skip it — `updatePeriodMillis` already refreshes daily. Report which you did.

- [ ] **Step 6: Build, install, verify the provider is registered**

Run: `./gradlew :app:installDebug` → BUILD SUCCESSFUL. Then:
```bash
adb shell dumpsys appwidget | grep -i domina
adb shell am start -n com.domina.cycle/.MainActivity   # app still launches
adb logcat -d | grep -iE "FATAL|AndroidRuntime|Glance" | tail
```
Expected: the `com.domina.cycle/.widget.CycleWidgetReceiver` provider is listed by `dumpsys appwidget`; app launches; no crash. (Adding the widget to the home screen is a manual gesture the user does; the provider being registered means it's available in the widget picker.)

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
        app/src/main/java/com/domina/cycle/widget/ app/src/main/res/xml/cycle_widget_info.xml \
        app/src/main/AndroidManifest.xml app/src/main/java/com/domina/cycle/reminders/DailyRecomputeWorker.kt
git commit -m "feat: Glance home-screen widget (cycle day / phase / countdown)"
```

---

## Phase 6d Definition of Done

- `WidgetData` pure logic is unit-tested (cycle, pregnancy, empty).
- The Glance widget compiles, installs, and its provider is registered (appears in the launcher's widget picker); tapping it opens the app; shows cycle day/phase (or pregnancy week/countdown) reading the encrypted DB via a Hilt EntryPoint.
- Full unit suite green; app installed at the end. Still **no `INTERNET` permission**.

## Deferred
- Multiple widget sizes/layouts; instant widget refresh on every log (currently daily + on data-worker run); themed widget colors matching the in-app theme.
```
