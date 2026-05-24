# Phase 3a: Reminders — Notifications, Exact Alarms & Background Recompute — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reliably remind the user about her cycle — period approaching/today/late, fertile window opening, ovulation day, and a daily logging nudge — via exact, reboot-surviving local notifications, each individually toggleable, recomputed in the background as her predictions change.

**Architecture:** A pure-Kotlin `ReminderScheduler` turns a `CyclePrediction` + user `ReminderSettings` into a list of timed `ScheduledReminder`s (fully unit-tested). Platform glue then fires them: `AlarmManager` exact alarms → a `ReminderReceiver` that posts notifications; a `BootReceiver` re-arms alarms after reboot; a daily `WorkManager` job recomputes predictions from the DB and reschedules. Settings (toggles + nudge time) persist in DataStore. No new networking — **still no `INTERNET` permission.**

**Tech Stack:** Kotlin, java.time, AlarmManager, NotificationManagerCompat + channels, WorkManager (HiltWorker), Hilt `@AndroidEntryPoint` receivers, DataStore, Jetpack Compose (Settings screen). Builds on Phase 1 + Phase 2 (on `main`).

> Domain unit tests (Task 1) are pure JVM — no device. Tasks 2–5 need the connected Galaxy S23 for manual verification (a notification actually appearing). **After any `connectedDebugAndroidTest`, run `./gradlew :app:installDebug` so the app stays on the phone.**

---

## Builds on (existing, on `main`)

- `domain/prediction/CyclePrediction.kt` — `cycleDay, phase, averageCycleLength, averagePeriodLength, nextPeriodDate, ovulationDate, fertileWindowStart, fertileWindowEnd, confidence`.
- `domain/prediction/{PeriodDeriver,CyclePredictor}.kt`; `data/model/{DayLog,FlowIntensity}`.
- `data/repository/DayLogRepository.kt` — `observeRange`, `getByDate`, `save`; `RoomDayLogRepository` (`@Inject`).
- `data/prefs/SettingsRepository.kt` — DataStore-backed (`theme`, `pinHash`); **extended here** with reminder settings.
- `di/{DatabaseModule,RepositoryModule}.kt`; `CycleApp` (`@HiltAndroidApp`); `MainActivity` (`@AndroidEntryPoint`, FragmentActivity).
- Build: Room 2.7.0 + **KSP**, Hilt 2.53.1, version catalog at `gradle/libs.versions.toml`.

## New file structure

```
app/src/main/java/com/domina/cycle/
  domain/reminders/
    ReminderType.kt          # enum of reminder kinds
    ReminderSettings.kt      # data class: toggles + nudge time + lead days
    ScheduledReminder.kt     # data class: type + fireAt + title + body
    ReminderScheduler.kt     # pure: prediction + settings + now -> List<ScheduledReminder>
  reminders/
    NotificationChannels.kt  # channel ids + creation
    Notifier.kt              # posts a notification for a ScheduledReminder
    ReminderReceiver.kt      # @AndroidEntryPoint BroadcastReceiver: alarm -> Notifier
    AlarmScheduler.kt        # sets/cancels exact alarms for a list of ScheduledReminders
    ReminderManager.kt       # recompute prediction from repo + reschedule (one entry point)
    BootReceiver.kt          # @AndroidEntryPoint: on boot -> ReminderManager.reschedule
    DailyRecomputeWorker.kt  # HiltWorker: daily -> ReminderManager.reschedule
  data/prefs/SettingsRepository.kt   # MODIFY: add reminder settings
  ui/settings/SettingsScreen.kt, SettingsViewModel.kt   # NEW
  ui/nav/{Destinations,AppNav}.kt    # MODIFY: add Settings route + nav item
  MainActivity.kt            # MODIFY: request POST_NOTIFICATIONS; kick reschedule
  di/WorkerModule.kt / CycleApp.kt   # MODIFY: WorkManager + Hilt worker factory
app/src/test/java/com/domina/cycle/domain/reminders/ReminderSchedulerTest.kt
```

---

## Task 1: ReminderScheduler (pure timing logic)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/reminders/ReminderType.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/reminders/ReminderSettings.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/reminders/ScheduledReminder.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/reminders/ReminderScheduler.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/reminders/ReminderSchedulerTest.kt`

Contract: `compute(prediction, settings, now)` returns only reminders with `fireAt > now`. All clock-time reminders fire at `settings.morningHour` (default 9) except the daily nudge (at `settings.dailyNudgeTime`). Period reminders need `nextPeriodDate`; fertility reminders need `fertileWindowStart`/`ovulationDate`. Each type appears at most once.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.reminders

import com.domina.cycle.domain.prediction.Confidence
import com.domina.cycle.domain.prediction.CyclePhase
import com.domina.cycle.domain.prediction.CyclePrediction
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderSchedulerTest {
    private fun pred(
        next: LocalDate? = LocalDate.parse("2026-06-01"),
        fertileStart: LocalDate? = LocalDate.parse("2026-05-13"),
        ovulation: LocalDate? = LocalDate.parse("2026-05-18"),
    ) = CyclePrediction(
        cycleDay = 9, phase = CyclePhase.FOLLICULAR,
        averageCycleLength = 28, averagePeriodLength = 5,
        nextPeriodDate = next, ovulationDate = ovulation,
        fertileWindowStart = fertileStart, fertileWindowEnd = ovulation?.plusDays(1),
        confidence = Confidence.HIGH,
    )
    private val allOn = ReminderSettings(
        periodAlerts = true, fertilityAlerts = true, dailyNudge = true,
        dailyNudgeTime = LocalTime.of(20, 0), periodLeadDays = 2, morningHour = 9,
    )
    private val now = LocalDateTime.parse("2026-05-10T07:00:00")

    @Test fun allDisabledProducesNothing() {
        val s = allOn.copy(periodAlerts = false, fertilityAlerts = false, dailyNudge = false)
        assertThat(ReminderScheduler.compute(pred(), s, now)).isEmpty()
    }

    @Test fun periodAlertsProduceSoonTodayLate() {
        val r = ReminderScheduler.compute(pred(), allOn.copy(fertilityAlerts = false, dailyNudge = false), now)
        val types = r.map { it.type }
        assertThat(types).containsExactly(
            ReminderType.PERIOD_SOON, ReminderType.PERIOD_TODAY, ReminderType.PERIOD_LATE)
        val soon = r.first { it.type == ReminderType.PERIOD_SOON }
        assertThat(soon.fireAt).isEqualTo(LocalDateTime.parse("2026-05-30T09:00:00")) // Jun1 - 2 @ 9
        assertThat(r.first { it.type == ReminderType.PERIOD_TODAY }.fireAt)
            .isEqualTo(LocalDateTime.parse("2026-06-01T09:00:00"))
        assertThat(r.first { it.type == ReminderType.PERIOD_LATE }.fireAt)
            .isEqualTo(LocalDateTime.parse("2026-06-03T09:00:00")) // +2 days
    }

    @Test fun fertilityAlertsProduceWindowAndOvulation() {
        val n = LocalDateTime.parse("2026-05-01T07:00:00")
        val r = ReminderScheduler.compute(pred(), allOn.copy(periodAlerts = false, dailyNudge = false), n)
        assertThat(r.map { it.type })
            .containsExactly(ReminderType.FERTILE_WINDOW_OPEN, ReminderType.OVULATION_DAY)
        assertThat(r.first { it.type == ReminderType.OVULATION_DAY }.fireAt)
            .isEqualTo(LocalDateTime.parse("2026-05-18T09:00:00"))
    }

    @Test fun dailyNudgeFiresTodayIfTimeNotYetPassed() {
        val r = ReminderScheduler.compute(pred(), allOn.copy(periodAlerts = false, fertilityAlerts = false), now)
        assertThat(r.single().type).isEqualTo(ReminderType.DAILY_LOG_NUDGE)
        assertThat(r.single().fireAt).isEqualTo(LocalDateTime.parse("2026-05-10T20:00:00"))
    }

    @Test fun dailyNudgeRollsToTomorrowIfTimePassed() {
        val late = LocalDateTime.parse("2026-05-10T21:00:00")
        val r = ReminderScheduler.compute(pred(), allOn.copy(periodAlerts = false, fertilityAlerts = false), late)
        assertThat(r.single().fireAt).isEqualTo(LocalDateTime.parse("2026-05-11T20:00:00"))
    }

    @Test fun pastRemindersAreExcluded() {
        // now is after Jun 1 -> PERIOD_SOON/TODAY are in the past, only LATE (Jun 3) remains
        val n = LocalDateTime.parse("2026-06-02T07:00:00")
        val r = ReminderScheduler.compute(pred(), allOn.copy(fertilityAlerts = false, dailyNudge = false), n)
        assertThat(r.map { it.type }).containsExactly(ReminderType.PERIOD_LATE)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ReminderSchedulerTest*"`
Expected: FAIL — unresolved `ReminderScheduler` / `ReminderType` / `ReminderSettings` / `ScheduledReminder`.

- [ ] **Step 3: Implement the four files**

`ReminderType.kt`:
```kotlin
package com.domina.cycle.domain.reminders

enum class ReminderType {
    PERIOD_SOON, PERIOD_TODAY, PERIOD_LATE,
    FERTILE_WINDOW_OPEN, OVULATION_DAY,
    DAILY_LOG_NUDGE,
}
```
`ReminderSettings.kt`:
```kotlin
package com.domina.cycle.domain.reminders

import java.time.LocalTime

data class ReminderSettings(
    val periodAlerts: Boolean = true,
    val fertilityAlerts: Boolean = true,
    val dailyNudge: Boolean = false,
    val dailyNudgeTime: LocalTime = LocalTime.of(20, 0),
    val periodLeadDays: Int = 2,
    val morningHour: Int = 9,
)
```
`ScheduledReminder.kt`:
```kotlin
package com.domina.cycle.domain.reminders

import java.time.LocalDateTime

data class ScheduledReminder(
    val type: ReminderType,
    val fireAt: LocalDateTime,
    val title: String,
    val body: String,
)
```
`ReminderScheduler.kt`:
```kotlin
package com.domina.cycle.domain.reminders

import com.domina.cycle.domain.prediction.CyclePrediction
import java.time.LocalDateTime

/** Pure timing logic: which reminders should fire, and when, given a prediction & settings. */
object ReminderScheduler {
    fun compute(
        prediction: CyclePrediction,
        settings: ReminderSettings,
        now: LocalDateTime,
    ): List<ScheduledReminder> {
        val out = mutableListOf<ScheduledReminder>()
        val morning = settings.morningHour

        if (settings.periodAlerts) {
            prediction.nextPeriodDate?.let { next ->
                out += ScheduledReminder(
                    ReminderType.PERIOD_SOON,
                    next.minusDays(settings.periodLeadDays.toLong()).atTime(morning, 0),
                    "Period coming up 🩸",
                    "Your period may start in about ${settings.periodLeadDays} days. A good time to stock up 💛",
                )
                out += ScheduledReminder(
                    ReminderType.PERIOD_TODAY, next.atTime(morning, 0),
                    "Period may start today 🩸", "Today's the day your period is predicted. Take it easy 💛",
                )
                out += ScheduledReminder(
                    ReminderType.PERIOD_LATE, next.plusDays(2).atTime(morning, 0),
                    "Period is a couple days late", "No period logged yet — tap to log, or just keep an eye out.",
                )
            }
        }
        if (settings.fertilityAlerts) {
            prediction.fertileWindowStart?.let {
                out += ScheduledReminder(
                    ReminderType.FERTILE_WINDOW_OPEN, it.atTime(morning, 0),
                    "Fertile window opening 🌸", "Your most fertile days are starting.",
                )
            }
            prediction.ovulationDate?.let {
                out += ScheduledReminder(
                    ReminderType.OVULATION_DAY, it.atTime(morning, 0),
                    "Ovulation day 🌸", "Estimated ovulation is today — peak fertility.",
                )
            }
        }
        if (settings.dailyNudge) {
            val todayAt = now.toLocalDate().atTime(settings.dailyNudgeTime)
            val fireAt = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
            out += ScheduledReminder(
                ReminderType.DAILY_LOG_NUDGE, fireAt,
                "How are you feeling today? 💛", "A quick tap to log your mood, flow, or symptoms.",
            )
        }
        return out.filter { it.fireAt.isAfter(now) }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*ReminderSchedulerTest*"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/reminders/ app/src/test/java/com/domina/cycle/domain/reminders/
git commit -m "feat: pure reminder scheduler (period/fertility/daily-nudge timing)"
```

---

## Task 2: Notification channels + Notifier + permission

**Files:**
- Create: `app/src/main/java/com/domina/cycle/reminders/NotificationChannels.kt`
- Create: `app/src/main/java/com/domina/cycle/reminders/Notifier.kt`
- Modify: `app/src/main/AndroidManifest.xml` (add `POST_NOTIFICATIONS`)
- Modify: `app/src/main/res/values/strings.xml` (no new strings strictly needed)

- [ ] **Step 1: Add the notification permission to the manifest**

In `AndroidManifest.xml`, add alongside the existing `USE_BIOMETRIC` permission:
```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

- [ ] **Step 2: Implement `NotificationChannels`**

```kotlin
package com.domina.cycle.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.domina.cycle.domain.reminders.ReminderType

object NotificationChannels {
    const val CYCLE = "cycle_alerts"
    const val FERTILITY = "fertility_alerts"
    const val NUDGE = "daily_nudge"

    fun channelFor(type: ReminderType): String = when (type) {
        ReminderType.PERIOD_SOON, ReminderType.PERIOD_TODAY, ReminderType.PERIOD_LATE -> CYCLE
        ReminderType.FERTILE_WINDOW_OPEN, ReminderType.OVULATION_DAY -> FERTILITY
        ReminderType.DAILY_LOG_NUDGE -> NUDGE
    }

    fun ensureCreated(context: Context) {
        val mgr = context.getSystemService<NotificationManager>() ?: return
        listOf(
            Triple(CYCLE, "Cycle & period alerts", NotificationManager.IMPORTANCE_HIGH),
            Triple(FERTILITY, "Fertility alerts", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(NUDGE, "Daily logging nudge", NotificationManager.IMPORTANCE_LOW),
        ).forEach { (id, name, importance) ->
            mgr.createNotificationChannel(NotificationChannel(id, name, importance))
        }
    }
}
```

- [ ] **Step 3: Implement `Notifier`**

```kotlin
package com.domina.cycle.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.domina.cycle.R
import com.domina.cycle.domain.reminders.ReminderType

class Notifier(private val context: Context) {

    fun notify(type: ReminderType, title: String, body: String) {
        NotificationChannels.ensureCreated(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) return // user hasn't granted notifications; skip silently

        val n = NotificationCompat.Builder(context, NotificationChannels.channelFor(type))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(type.ordinal, n)
    }
}
```

- [ ] **Step 4: Add a notification icon**

Create `app/src/main/res/drawable/ic_notification.xml` (a simple vector; notification icons should be white-on-transparent):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24" android:tint="#FFFFFF">
    <path android:fillColor="#FFFFFF"
        android:pathData="M12,2A10,10 0,1 0,22 12,10 10,0 0,0 12,2Zm0,18a8,8 0,1 1,8 -8,8 8,0 0,1 -8,8Zm1,-13h-2v6h2Zm0,8h-2v2h2Z"/>
</vector>
```

- [ ] **Step 5: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/domina/cycle/reminders/NotificationChannels.kt \
        app/src/main/java/com/domina/cycle/reminders/Notifier.kt \
        app/src/main/res/drawable/ic_notification.xml app/src/main/AndroidManifest.xml
git commit -m "feat: notification channels, Notifier, and POST_NOTIFICATIONS permission"
```

---

## Task 3: Exact-alarm scheduling + ReminderReceiver

**Files:**
- Create: `app/src/main/java/com/domina/cycle/reminders/ReminderReceiver.kt`
- Create: `app/src/main/java/com/domina/cycle/reminders/AlarmScheduler.kt`
- Modify: `app/src/main/AndroidManifest.xml` (register receiver; add `USE_EXACT_ALARM`)

- [ ] **Step 1: Add exact-alarm permission and register the receiver in the manifest**

Add permission (granted by default; appropriate for a reminder-centric app):
```xml
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
```
Inside `<application>`:
```xml
        <receiver android:name=".reminders.ReminderReceiver" android:exported="false" />
```

- [ ] **Step 2: Implement `ReminderReceiver` (@AndroidEntryPoint)**

```kotlin
package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.domina.cycle.domain.reminders.ReminderType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE)?.let { ReminderType.valueOf(it) } ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return
        Notifier(context).notify(type, title, body)
    }

    companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }
}
```

- [ ] **Step 3: Implement `AlarmScheduler`**

```kotlin
package com.domina.cycle.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.domina.cycle.domain.reminders.ReminderType
import com.domina.cycle.domain.reminders.ScheduledReminder
import java.time.ZoneId

/** Schedules/cancels exact alarms for the given reminders. One alarm per ReminderType. */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService<AlarmManager>()!!

    fun reschedule(reminders: List<ScheduledReminder>) {
        ReminderType.entries.forEach { cancel(it) }
        reminders.forEach { schedule(it) }
    }

    private fun schedule(r: ScheduledReminder) {
        val triggerAt = r.fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(r))
    }

    private fun cancel(type: ReminderType) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context, type.ordinal,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return
        )
    }

    private fun pendingIntent(r: ScheduledReminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TYPE, r.type.name)
            putExtra(ReminderReceiver.EXTRA_TITLE, r.title)
            putExtra(ReminderReceiver.EXTRA_BODY, r.body)
        }
        return PendingIntent.getBroadcast(
            context, r.type.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
```

- [ ] **Step 4: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual device verification (a real notification fires)**

Install and trigger a near-future alarm via a temporary test, OR verify end-to-end after Task 4 wires `ReminderManager`. Minimum here: `./gradlew :app:installDebug` succeeds and the app launches without crash (`adb shell am start -n com.domina.cycle/.MainActivity`; check `adb logcat -d` for no `FATAL`). Full "notification appears" verification happens in Task 4 Step 6.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/domina/cycle/reminders/ReminderReceiver.kt \
        app/src/main/java/com/domina/cycle/reminders/AlarmScheduler.kt \
        app/src/main/AndroidManifest.xml
git commit -m "feat: exact-alarm scheduling and reminder broadcast receiver"
```

---

## Task 4: ReminderManager, BootReceiver, daily WorkManager recompute

**Files:**
- Modify: `gradle/libs.versions.toml` + `app/build.gradle.kts` (add WorkManager + Hilt-work)
- Create: `app/src/main/java/com/domina/cycle/reminders/ReminderManager.kt`
- Create: `app/src/main/java/com/domina/cycle/reminders/BootReceiver.kt`
- Create: `app/src/main/java/com/domina/cycle/reminders/DailyRecomputeWorker.kt`
- Modify: `app/src/main/java/com/domina/cycle/CycleApp.kt` (WorkManager Hilt config + schedule daily work)
- Modify: `app/src/main/AndroidManifest.xml` (BootReceiver + RECEIVE_BOOT_COMPLETED; disable default WorkManager initializer)

- [ ] **Step 1: Add dependencies**

In `gradle/libs.versions.toml` `[versions]`: `work = "2.10.0"` and `hiltExt = "1.2.0"`. In `[libraries]`:
```toml
work-runtime = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }
hilt-work = { module = "androidx.hilt:hilt-work", version.ref = "hiltExt" }
hilt-ext-compiler = { module = "androidx.hilt:hilt-compiler", version.ref = "hiltExt" }
```
In `app/build.gradle.kts` dependencies:
```kotlin
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)
```

- [ ] **Step 2: Implement `ReminderManager` (recompute + reschedule)**

```kotlin
package com.domina.cycle.reminders

import android.content.Context
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import com.domina.cycle.domain.reminders.ReminderScheduler
import com.domina.cycle.domain.reminders.ReminderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** Reads logs + settings, recomputes the prediction, and (re)schedules all alarms. */
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DayLogRepository,
    private val settingsProvider: ReminderSettingsProvider,
) {
    suspend fun reschedule() {
        val today = LocalDate.now()
        val logs = repository.observeRange(today.minusDays(400), today).first()
        val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
        val prediction = CyclePredictor.predict(periods, today)
        val settings: ReminderSettings = settingsProvider.current()
        val reminders = ReminderScheduler.compute(prediction, settings, LocalDateTime.now())
        AlarmScheduler(context).reschedule(reminders)
    }
}
```

`ReminderSettingsProvider` is a thin interface over `SettingsRepository.reminderSettings` (added in Task 5). Define it now so this compiles:
```kotlin
package com.domina.cycle.reminders

import com.domina.cycle.domain.reminders.ReminderSettings

interface ReminderSettingsProvider { suspend fun current(): ReminderSettings }
```

- [ ] **Step 3: Implement `BootReceiver`**

```kotlin
package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var reminderManager: ReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try { reminderManager.reschedule() } finally { pending.finish() }
        }
    }
}
```

- [ ] **Step 4: Implement `DailyRecomputeWorker` (HiltWorker)**

```kotlin
package com.domina.cycle.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyRecomputeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderManager: ReminderManager,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        reminderManager.reschedule()
        return Result.success()
    }
}
```

- [ ] **Step 5: Wire WorkManager + Hilt in `CycleApp` and the manifest**

`CycleApp.kt`:
```kotlin
package com.domina.cycle

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.domina.cycle.reminders.DailyRecomputeWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CycleApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_recompute",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyRecomputeWorker>(1, TimeUnit.DAYS).build(),
        )
    }
}
```
In `AndroidManifest.xml`: add `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />`, register the boot receiver, and disable the default WorkManager initializer (required when the app provides its own `Configuration`):
```xml
        <receiver android:name=".reminders.BootReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
```
Add `xmlns:tools="http://schemas.android.com/tools"` to the `<manifest>` root if not present.

- [ ] **Step 6: Build, install, and verify a real notification fires**

Run: `./gradlew :app:installDebug` (Expected: BUILD SUCCESSFUL, installed).
Manual end-to-end check (do this with a temporary 1-minute alarm OR by adb-broadcasting the receiver directly):
```bash
adb shell am broadcast -n com.domina.cycle/.reminders.ReminderReceiver \
  -e type DAILY_LOG_NUDGE -e title "Test 💛" -e body "If you see this, reminders work."
```
Expected: a heads-up notification "Test 💛" appears on the phone (grant the notification permission first if prompted — see Task 5). Check `adb logcat -d` for no crash.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
        app/src/main/java/com/domina/cycle/reminders/ReminderManager.kt \
        app/src/main/java/com/domina/cycle/reminders/ReminderSettingsProvider.kt \
        app/src/main/java/com/domina/cycle/reminders/BootReceiver.kt \
        app/src/main/java/com/domina/cycle/reminders/DailyRecomputeWorker.kt \
        app/src/main/java/com/domina/cycle/CycleApp.kt app/src/main/AndroidManifest.xml
git commit -m "feat: reschedule on boot + daily WorkManager recompute via ReminderManager"
```

---

## Task 5: Reminder settings, Settings screen, permission request + reschedule wiring

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/data/prefs/SettingsRepository.kt` (reminder settings + implement `ReminderSettingsProvider`)
- Create: `app/src/main/java/com/domina/cycle/ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt`
- Modify: `app/src/main/java/com/domina/cycle/ui/nav/Destinations.kt`, `AppNav.kt`
- Modify: `app/src/main/java/com/domina/cycle/MainActivity.kt` (request POST_NOTIFICATIONS; reschedule on resume)
- Create: `app/src/main/java/com/domina/cycle/di/ReminderModule.kt` (bind `ReminderSettingsProvider`)
- Test: `app/src/test/java/com/domina/cycle/data/prefs/ReminderSettingsMappingTest.kt`

- [ ] **Step 1: Write a failing unit test for the settings encode/decode mapping**

```kotlin
package com.domina.cycle.data.prefs

import com.domina.cycle.domain.reminders.ReminderSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

class ReminderSettingsMappingTest {
    @Test fun nudgeTimeRoundTripsAsMinutesOfDay() {
        val t = LocalTime.of(20, 30)
        assertThat(ReminderSettingsCodec.timeToMinutes(t)).isEqualTo(1230)
        assertThat(ReminderSettingsCodec.minutesToTime(1230)).isEqualTo(t)
    }

    @Test fun defaultsAreReasonable() {
        val s = ReminderSettings()
        assertThat(s.periodAlerts).isTrue()
        assertThat(s.dailyNudge).isFalse()
        assertThat(s.dailyNudgeTime).isEqualTo(LocalTime.of(20, 0))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ReminderSettingsMappingTest*"`
Expected: FAIL — unresolved `ReminderSettingsCodec`.

- [ ] **Step 3: Add `ReminderSettingsCodec` and extend `SettingsRepository`**

Create `app/src/main/java/com/domina/cycle/data/prefs/ReminderSettingsCodec.kt`:
```kotlin
package com.domina.cycle.data.prefs

import java.time.LocalTime

object ReminderSettingsCodec {
    fun timeToMinutes(t: LocalTime): Int = t.hour * 60 + t.minute
    fun minutesToTime(m: Int): LocalTime = LocalTime.of(m / 60, m % 60)
}
```
Append to `SettingsRepository` (keep the existing theme/pin code):
```kotlin
    // --- reminder settings ---
    private val periodAlertsKey = booleanPreferencesKey("rem_period")
    private val fertilityAlertsKey = booleanPreferencesKey("rem_fertility")
    private val dailyNudgeKey = booleanPreferencesKey("rem_nudge")
    private val nudgeMinsKey = intPreferencesKey("rem_nudge_mins")

    val reminderSettings: Flow<com.domina.cycle.domain.reminders.ReminderSettings> =
        context.dataStore.data.map { p ->
            com.domina.cycle.domain.reminders.ReminderSettings(
                periodAlerts = p[periodAlertsKey] ?: true,
                fertilityAlerts = p[fertilityAlertsKey] ?: true,
                dailyNudge = p[dailyNudgeKey] ?: false,
                dailyNudgeTime = ReminderSettingsCodec.minutesToTime(p[nudgeMinsKey] ?: (20 * 60)),
            )
        }

    suspend fun setReminderSettings(s: com.domina.cycle.domain.reminders.ReminderSettings) {
        context.dataStore.edit {
            it[periodAlertsKey] = s.periodAlerts
            it[fertilityAlertsKey] = s.fertilityAlerts
            it[dailyNudgeKey] = s.dailyNudge
            it[nudgeMinsKey] = ReminderSettingsCodec.timeToMinutes(s.dailyNudgeTime)
        }
    }
```
Add the imports `androidx.datastore.preferences.core.booleanPreferencesKey` and `intPreferencesKey` to `SettingsRepository.kt`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*ReminderSettingsMappingTest*"`
Expected: PASS.

- [ ] **Step 5: Implement `ReminderSettingsProvider` binding**

Create `app/src/main/java/com/domina/cycle/data/prefs/SettingsReminderProvider.kt`:
```kotlin
package com.domina.cycle.data.prefs

import com.domina.cycle.domain.reminders.ReminderSettings
import com.domina.cycle.reminders.ReminderSettingsProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SettingsReminderProvider @Inject constructor(
    private val settings: SettingsRepository,
) : ReminderSettingsProvider {
    override suspend fun current(): ReminderSettings = settings.reminderSettings.first()
}
```
Create `app/src/main/java/com/domina/cycle/di/ReminderModule.kt`:
```kotlin
package com.domina.cycle.di

import com.domina.cycle.data.prefs.SettingsReminderProvider
import com.domina.cycle.reminders.ReminderSettingsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds @Singleton
    abstract fun bindReminderSettingsProvider(impl: SettingsReminderProvider): ReminderSettingsProvider
}
```

- [ ] **Step 6: Implement `SettingsViewModel` and `SettingsScreen`**

`SettingsViewModel.kt`:
```kotlin
package com.domina.cycle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.data.prefs.ThemePreference
import com.domina.cycle.domain.reminders.ReminderSettings
import com.domina.cycle.reminders.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val reminderManager: ReminderManager,
) : ViewModel() {
    val theme: StateFlow<ThemePreference> =
        settings.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreference.SOFT_SWEET)
    val reminders: StateFlow<ReminderSettings> =
        settings.reminderSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderSettings())

    fun setTheme(t: ThemePreference) { viewModelScope.launch { settings.setTheme(t) } }

    fun updateReminders(s: ReminderSettings) {
        viewModelScope.launch { settings.setReminderSettings(s); reminderManager.reschedule() }
    }
}
```
`SettingsScreen.kt`:
```kotlin
package com.domina.cycle.ui.settings

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
import com.domina.cycle.data.prefs.ThemePreference

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val rem by vm.reminders.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        ThemePreference.entries.forEach { t ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = theme == t, onClick = { vm.setTheme(t) })
                Text(t.displayName)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Reminders", style = MaterialTheme.typography.titleMedium)
        ToggleRow("Period & cycle alerts", rem.periodAlerts) { vm.updateReminders(rem.copy(periodAlerts = it)) }
        ToggleRow("Fertility alerts", rem.fertilityAlerts) { vm.updateReminders(rem.copy(fertilityAlerts = it)) }
        ToggleRow("Daily logging nudge (8 PM)", rem.dailyNudge) { vm.updateReminders(rem.copy(dailyNudge = it)) }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
```
(The nudge time picker is kept simple — fixed copy "8 PM" matching the default; a time picker can be added later. The toggle persists and reschedules.)

- [ ] **Step 7: Add the Settings route + nav, and request notification permission in MainActivity**

In `Destinations.kt` add `const val SETTINGS = "settings"`. In `AppNav.kt` add a third `NavigationBarItem` (icon `Icons.Filled.Settings`) navigating to `SETTINGS`, and a `composable(Destinations.SETTINGS) { SettingsScreen() }`.
In `MainActivity.kt`, after unlock, request `POST_NOTIFICATIONS` on API 33+ using `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` and trigger it once; and call a reschedule on resume. Minimal addition inside the `AppTheme { if (unlocked) { ... } }` branch:
```kotlin
// at top of the composable, after unlocked is known:
val notifLauncher = rememberLauncherForActivityResult(
    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
) {}
LaunchedEffect(unlocked) {
    if (unlocked && android.os.Build.VERSION.SDK_INT >= 33) {
        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}
```
And inject `ReminderManager` into MainActivity (`@Inject lateinit var reminderManager: ReminderManager`) and call `lifecycleScope.launch { reminderManager.reschedule() }` once after unlock so alarms are armed from current data.

- [ ] **Step 8: Build, run full unit suite, install, and verify end-to-end**

Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
```
On device: open app → unlock → grant the notification prompt → go to Settings, toggle "Daily logging nudge" on. Then verify a notification fires using the broadcast from Task 4 Step 6 (or set the nudge time a minute ahead). Confirm `adb logcat -d` shows no crash.

- [ ] **Step 9: Reinstall so the app stays on the phone, then commit**

```bash
./gradlew :app:installDebug   # ensure present after any connected tests
git add app/src/main/java/com/domina/cycle/data/prefs/ \
        app/src/main/java/com/domina/cycle/ui/settings/ \
        app/src/main/java/com/domina/cycle/di/ReminderModule.kt \
        app/src/main/java/com/domina/cycle/ui/nav/ \
        app/src/main/java/com/domina/cycle/MainActivity.kt \
        app/src/test/java/com/domina/cycle/data/prefs/ReminderSettingsMappingTest.kt
git commit -m "feat: reminder settings, Settings screen, notification permission + reschedule wiring"
```

---

## Phase 3a Definition of Done

- `ReminderScheduler` timing logic is pure Kotlin with passing unit tests; settings mapping unit-tested.
- A notification provably fires on the device (via the test broadcast and/or a near-future alarm).
- Reminders reschedule after logging/app-open, on reboot (`BootReceiver`), and daily (`WorkManager`).
- Each reminder category is individually toggleable in a Settings screen; choices persist.
- Full unit suite green; Phase 1 instrumented suite still green; app installed on the device at the end.
- Still **no `INTERNET` permission** (only `USE_BIOMETRIC`, `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`).

## Deferred to Phase 3b

- **Meds & appointment reminders** — need their own entities + entry UI (a medications list and an appointments list), then reuse this phase's `Notifier`/`AlarmScheduler` infrastructure.
- A proper time picker for the daily nudge (Phase 3a ships a fixed default time toggle).
- Notification deep-links that open the relevant screen (Log/Today) on tap.
```
