# Phase 3b: Medication & Appointment Reminders — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user add medications (e.g., prenatal vitamins, the pill) that remind her daily at a chosen time, and appointments (e.g., doctor visits) that remind her ahead of time — reusing Phase 3a's notification + exact-alarm engine.

**Architecture:** Two new encrypted Room tables (`medications`, `appointments`) added via a **non-destructive Migration(1→2)** (the device already holds real data). A pure-Kotlin `MedAppointmentScheduler` turns them into timed `ScheduledItem`s (unit-tested). A generalized `ItemAlarmScheduler` + `ItemReminderReceiver` fire them through the existing `Notifier` on a new channel; `ReminderManager` schedules them alongside cycle reminders. Two simple add/list/delete screens manage them.

**Tech Stack:** Kotlin, Room 2.7.0 + KSP, java.time, AlarmManager, NotificationManagerCompat, Hilt, Compose. Builds on Phases 1–3a + 4a (on `main`).

> Tasks 2 is pure-JVM unit-tested. Tasks 1, 3, 4, 5 need the connected Galaxy S23. **CRITICAL:** the device holds a real v1 database — the migration MUST be non-destructive; verify existing cycle data survives. After any `connectedDebugAndroidTest`, run `./gradlew :app:installDebug`.

## Builds on (existing, on `main`)
- `data/db/AppDatabase.kt` (version 1, entities DayLogEntity + CycleEventEntity, `@TypeConverters(Converters)`), `di/DatabaseModule.kt` (Room + SQLCipher), `data/db/...Dao`.
- `reminders/`: `Notifier`, `NotificationChannels`, `AlarmScheduler`, `ReminderReceiver`, `ReminderManager` (recompute+reschedule), `domain/reminders/ScheduledReminder`.
- `ui/nav/{Destinations,AppNav}.kt`; `ui/settings/...`.

## New file structure
```
app/src/main/java/com/domina/cycle/
  data/db/entity/MedicationEntity.kt, AppointmentEntity.kt
  data/db/MedicationDao.kt, AppointmentDao.kt
  data/db/Migrations.kt              # MIGRATION_1_2
  data/repository/MedicationRepository.kt (+ Room impl), AppointmentRepository.kt (+ Room impl)
  domain/reminders/ScheduledItem.kt  # generic id-keyed scheduled notification
  domain/reminders/MedAppointmentScheduler.kt  # pure
  reminders/ItemAlarmScheduler.kt, ItemReminderReceiver.kt
  ui/meds/MedsViewModel.kt, MedsScreen.kt
  ui/appointments/AppointmentsViewModel.kt, AppointmentsScreen.kt
app/src/test/java/com/domina/cycle/domain/reminders/MedAppointmentSchedulerTest.kt
app/src/androidTest/java/com/domina/cycle/data/db/MedsAppointmentsDaoTest.kt
```

---

## Task 1: Entities, DAOs, non-destructive migration, repositories

**Files:**
- Create: `data/db/entity/MedicationEntity.kt`, `entity/AppointmentEntity.kt`, `MedicationDao.kt`, `AppointmentDao.kt`, `Migrations.kt`
- Create: `data/repository/MedicationRepository.kt`, `AppointmentRepository.kt`
- Modify: `data/db/AppDatabase.kt` (version 2 + new entities/DAOs), `di/DatabaseModule.kt` (addMigrations + provide new DAOs), `di/RepositoryModule.kt` (bind new repos)
- Test: `app/src/androidTest/java/com/domina/cycle/data/db/MedsAppointmentsDaoTest.kt`

- [ ] **Step 1: Write the failing instrumented test (in-memory Room CRUD)**

```kotlin
package com.domina.cycle.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.entity.AppointmentEntity
import com.domina.cycle.data.db.entity.MedicationEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedsAppointmentsDaoTest {
    private lateinit var db: AppDatabase
    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
    }
    @After fun tearDown() = db.close()

    @Test fun medicationCrud(): Unit = runBlocking {
        val id = db.medicationDao().insert(MedicationEntity(name = "Prenatal vitamin", timeMinutes = 540, enabled = true))
        assertThat(db.medicationDao().observeAll().first().single().name).isEqualTo("Prenatal vitamin")
        db.medicationDao().deleteById(id)
        assertThat(db.medicationDao().observeAll().first()).isEmpty()
    }

    @Test fun appointmentCrud(): Unit = runBlocking {
        val id = db.appointmentDao().insert(
            AppointmentEntity(title = "OB checkup", atEpochMillis = 1_900_000_000_000L, note = "", leadHours = 24, enabled = true))
        assertThat(db.appointmentDao().observeAll().first().single().title).isEqualTo("OB checkup")
        db.appointmentDao().deleteById(id)
        assertThat(db.appointmentDao().observeAll().first()).isEmpty()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.domina.cycle.data.db.MedsAppointmentsDaoTest`
Expected: FAIL — unresolved `MedicationEntity` etc.

- [ ] **Step 3: Implement entities and DAOs**

`entity/MedicationEntity.kt`:
```kotlin
package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timeMinutes: Int,   // minutes-of-day for the daily reminder
    val enabled: Boolean = true,
)
```
`entity/AppointmentEntity.kt`:
```kotlin
package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val atEpochMillis: Long,
    val note: String = "",
    val leadHours: Int = 24,
    val enabled: Boolean = true,
)
```
`MedicationDao.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert suspend fun insert(entity: MedicationEntity): Long
    @Query("SELECT * FROM medications ORDER BY timeMinutes") fun observeAll(): Flow<List<MedicationEntity>>
    @Query("DELETE FROM medications WHERE id = :id") suspend fun deleteById(id: Long)
}
```
`AppointmentDao.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Insert suspend fun insert(entity: AppointmentEntity): Long
    @Query("SELECT * FROM appointments ORDER BY atEpochMillis") fun observeAll(): Flow<List<AppointmentEntity>>
    @Query("DELETE FROM appointments WHERE id = :id") suspend fun deleteById(id: Long)
}
```

- [ ] **Step 4: Implement the migration and bump the database**

`Migrations.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `medications` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `timeMinutes` INTEGER NOT NULL, `enabled` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `appointments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `atEpochMillis` INTEGER NOT NULL, " +
                "`note` TEXT NOT NULL, `leadHours` INTEGER NOT NULL, `enabled` INTEGER NOT NULL)"
        )
    }
}
```
Modify `AppDatabase.kt`: bump `version = 2`, add the new entities and DAO accessors:
```kotlin
@Database(
    entities = [DayLogEntity::class, CycleEventEntity::class,
        com.domina.cycle.data.db.entity.MedicationEntity::class,
        com.domina.cycle.data.db.entity.AppointmentEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun cycleEventDao(): CycleEventDao
    abstract fun medicationDao(): MedicationDao
    abstract fun appointmentDao(): AppointmentDao
}
```
Modify `DatabaseModule.kt`: add `.addMigrations(MIGRATION_1_2)` to the `Room.databaseBuilder(...)` chain (before `.build()`), and add provider functions:
```kotlin
    @Provides fun provideMedicationDao(db: AppDatabase): MedicationDao = db.medicationDao()
    @Provides fun provideAppointmentDao(db: AppDatabase): AppointmentDao = db.appointmentDao()
```

> If Room reports a schema-validation error like "Expected: ... Found: ...", copy the EXACT `CREATE TABLE` text from the "Expected" section into the migration — that is Room's source of truth for the generated schema.

- [ ] **Step 5: Implement repositories + bind them**

`MedicationRepository.kt`:
```kotlin
package com.domina.cycle.data.repository

import com.domina.cycle.data.db.MedicationDao
import com.domina.cycle.data.db.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface MedicationRepository {
    fun observeAll(): Flow<List<MedicationEntity>>
    suspend fun add(name: String, timeMinutes: Int): Long
    suspend fun delete(id: Long)
}

class RoomMedicationRepository @Inject constructor(private val dao: MedicationDao) : MedicationRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun add(name: String, timeMinutes: Int) =
        dao.insert(MedicationEntity(name = name, timeMinutes = timeMinutes, enabled = true))
    override suspend fun delete(id: Long) = dao.deleteById(id)
}
```
`AppointmentRepository.kt`:
```kotlin
package com.domina.cycle.data.repository

import com.domina.cycle.data.db.AppointmentDao
import com.domina.cycle.data.db.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AppointmentRepository {
    fun observeAll(): Flow<List<AppointmentEntity>>
    suspend fun add(title: String, atEpochMillis: Long, leadHours: Int): Long
    suspend fun delete(id: Long)
}

class RoomAppointmentRepository @Inject constructor(private val dao: AppointmentDao) : AppointmentRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun add(title: String, atEpochMillis: Long, leadHours: Int) =
        dao.insert(AppointmentEntity(title = title, atEpochMillis = atEpochMillis, leadHours = leadHours, enabled = true))
    override suspend fun delete(id: Long) = dao.deleteById(id)
}
```
Append to `RepositoryModule.kt`:
```kotlin
    @Binds @Singleton
    abstract fun bindMedicationRepository(impl: RoomMedicationRepository): MedicationRepository

    @Binds @Singleton
    abstract fun bindAppointmentRepository(impl: RoomAppointmentRepository): AppointmentRepository
```

- [ ] **Step 6: Run the instrumented test to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.domina.cycle.data.db.MedsAppointmentsDaoTest`
Expected: PASS (2 tests).

- [ ] **Step 7: Verify the migration is non-destructive on the REAL device data**

The phone holds a v1 database with real/seeded cycle logs. Install the v2 app over it and confirm no migration crash and that existing data survives:
```bash
./gradlew :app:installDebug
adb shell am start -n com.domina.cycle/.MainActivity
adb logcat -d | grep -iE "migrat|IllegalStateException|Room|FATAL" | tail -20
```
Expected: no migration error / no crash. (After unlocking, the Today cycle dashboard should still show the previously logged data.)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/domina/cycle/data/db/ app/src/main/java/com/domina/cycle/data/repository/MedicationRepository.kt \
        app/src/main/java/com/domina/cycle/data/repository/AppointmentRepository.kt \
        app/src/main/java/com/domina/cycle/di/ app/src/androidTest/java/com/domina/cycle/data/db/MedsAppointmentsDaoTest.kt
git commit -m "feat: medications & appointments tables with non-destructive migration to v2"
```

---

## Task 2: MedAppointmentScheduler (pure timing)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/reminders/ScheduledItem.kt`
- Create: `app/src/main/java/com/domina/cycle/domain/reminders/MedAppointmentScheduler.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/reminders/MedAppointmentSchedulerTest.kt`

The scheduler takes plain inputs (decoupled from Room entities) so it stays pure: lists of `MedInput(id, name, timeMinutes, enabled)` and `ApptInput(id, title, atEpochMillis, leadHours, enabled)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.reminders

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class MedAppointmentSchedulerTest {
    private fun millis(dt: String) = LocalDateTime.parse(dt).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val now = LocalDateTime.parse("2026-05-10T07:00:00")

    @Test fun enabledMedSchedulesNextOccurrenceOfTime() {
        val items = MedAppointmentScheduler.compute(
            meds = listOf(MedAppointmentScheduler.MedInput(1, "Vitamin", 9 * 60, true)),
            appts = emptyList(), now = now, zone = ZoneOffset.UTC,
        )
        val it = items.single()
        assertThat(it.key).isEqualTo("med_1")
        assertThat(it.fireAt).isEqualTo(LocalDateTime.parse("2026-05-10T09:00:00")) // today, 9am ahead of 7am
        assertThat(it.title).contains("Vitamin")
    }

    @Test fun medTimeAlreadyPassedRollsToTomorrow() {
        val items = MedAppointmentScheduler.compute(
            meds = listOf(MedAppointmentScheduler.MedInput(1, "Pill", 6 * 60, true)),
            appts = emptyList(), now = now, zone = ZoneOffset.UTC,
        )
        assertThat(items.single().fireAt).isEqualTo(LocalDateTime.parse("2026-05-11T06:00:00"))
    }

    @Test fun disabledItemsAreSkipped() {
        val items = MedAppointmentScheduler.compute(
            meds = listOf(MedAppointmentScheduler.MedInput(1, "X", 9 * 60, false)),
            appts = listOf(MedAppointmentScheduler.ApptInput(2, "Y", millis("2026-06-01T10:00:00"), 24, false)),
            now = now, zone = ZoneOffset.UTC,
        )
        assertThat(items).isEmpty()
    }

    @Test fun appointmentFiresLeadHoursBefore() {
        val items = MedAppointmentScheduler.compute(
            meds = emptyList(),
            appts = listOf(MedAppointmentScheduler.ApptInput(7, "OB visit", millis("2026-06-01T10:00:00"), 24, true)),
            now = now, zone = ZoneOffset.UTC,
        )
        val it = items.single()
        assertThat(it.key).isEqualTo("appt_7")
        assertThat(it.fireAt).isEqualTo(LocalDateTime.parse("2026-05-31T10:00:00")) // 24h before
    }

    @Test fun pastAppointmentsAreSkipped() {
        val items = MedAppointmentScheduler.compute(
            meds = emptyList(),
            appts = listOf(MedAppointmentScheduler.ApptInput(7, "Old", millis("2026-05-01T10:00:00"), 24, true)),
            now = now, zone = ZoneOffset.UTC,
        )
        assertThat(items).isEmpty()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*MedAppointmentSchedulerTest*"`
Expected: FAIL — unresolved refs.

- [ ] **Step 3: Implement `ScheduledItem` and `MedAppointmentScheduler`**

`ScheduledItem.kt`:
```kotlin
package com.domina.cycle.domain.reminders

import java.time.LocalDateTime

/** A generic, id-keyed scheduled notification (used for meds & appointments). */
data class ScheduledItem(
    val key: String,        // stable, e.g. "med_3" / "appt_7"
    val fireAt: LocalDateTime,
    val channelId: String,
    val title: String,
    val body: String,
)
```
`MedAppointmentScheduler.kt`:
```kotlin
package com.domina.cycle.domain.reminders

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object MedAppointmentScheduler {
    const val CHANNEL = "meds_appointments"

    data class MedInput(val id: Long, val name: String, val timeMinutes: Int, val enabled: Boolean)
    data class ApptInput(val id: Long, val title: String, val atEpochMillis: Long, val leadHours: Int, val enabled: Boolean)

    fun compute(meds: List<MedInput>, appts: List<ApptInput>, now: LocalDateTime, zone: ZoneId): List<ScheduledItem> {
        val out = mutableListOf<ScheduledItem>()
        meds.filter { it.enabled }.forEach { m ->
            val today = now.toLocalDate().atTime(m.timeMinutes / 60, m.timeMinutes % 60)
            val fireAt = if (today.isAfter(now)) today else today.plusDays(1)
            out += ScheduledItem("med_${m.id}", fireAt, CHANNEL, "Time for ${m.name} 💊", "A gentle reminder to take ${m.name}.")
        }
        appts.filter { it.enabled }.forEach { a ->
            val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(a.atEpochMillis), zone)
            val fireAt = at.minusHours(a.leadHours.toLong())
            if (fireAt.isAfter(now)) {
                out += ScheduledItem("appt_${a.id}", fireAt, CHANNEL, "Upcoming: ${a.title} 📅",
                    "You have \"${a.title}\" in about ${a.leadHours} hours.")
            }
        }
        return out
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*MedAppointmentSchedulerTest*"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/reminders/ScheduledItem.kt \
        app/src/main/java/com/domina/cycle/domain/reminders/MedAppointmentScheduler.kt \
        app/src/test/java/com/domina/cycle/domain/reminders/MedAppointmentSchedulerTest.kt
git commit -m "feat: pure med & appointment reminder scheduler"
```

---

## Task 3: Item alarm scheduling + channel + ReminderManager integration

**Files:**
- Create: `app/src/main/java/com/domina/cycle/reminders/ItemReminderReceiver.kt`, `ItemAlarmScheduler.kt`
- Modify: `app/src/main/java/com/domina/cycle/reminders/NotificationChannels.kt` (add meds_appointments channel), `Notifier.kt` (raw channel method)
- Modify: `app/src/main/AndroidManifest.xml` (register ItemReminderReceiver)
- Modify: `app/src/main/java/com/domina/cycle/reminders/ReminderManager.kt` (also schedule items)

- [ ] **Step 1: Add the channel and a raw notify method**

In `NotificationChannels.kt` add `const val MEDS = "meds_appointments"` and register it in `ensureCreated` (importance DEFAULT, name "Medications & appointments").
In `Notifier.kt` add a method that posts by channel id directly (keep the existing typed `notify`):
```kotlin
    fun notifyRaw(channelId: String, notificationId: Int, title: String, body: String) {
        NotificationChannels.ensureCreated(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val n = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.domina.cycle.R.drawable.ic_notification)
            .setContentTitle(title).setContentText(body)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true).setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH).build()
        androidx.core.app.NotificationManagerCompat.from(context).notify(notificationId, n)
    }
```

- [ ] **Step 2: Implement `ItemReminderReceiver` and `ItemAlarmScheduler`**

`ItemReminderReceiver.kt`:
```kotlin
package com.domina.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ItemReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 1000)
        Notifier(context).notifyRaw(channel, notifId, title, body)
    }
    companion object {
        const val EXTRA_CHANNEL = "channel"; const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"; const val EXTRA_NOTIF_ID = "notif_id"
    }
}
```
`ItemAlarmScheduler.kt` (request codes are derived from the stable key; offset away from Phase 3a's `ReminderType.ordinal` codes):
```kotlin
package com.domina.cycle.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.domina.cycle.domain.reminders.ScheduledItem
import java.time.ZoneId

class ItemAlarmScheduler(private val context: Context) {
    private val am = context.getSystemService<AlarmManager>()!!

    fun scheduleAll(items: List<ScheduledItem>) = items.forEach { schedule(it) }

    fun cancel(key: String) {
        PendingIntent.getBroadcast(
            context, requestCode(key), Intent(context, ItemReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { am.cancel(it) }
    }

    private fun schedule(item: ScheduledItem) {
        val intent = Intent(context, ItemReminderReceiver::class.java).apply {
            putExtra(ItemReminderReceiver.EXTRA_CHANNEL, item.channelId)
            putExtra(ItemReminderReceiver.EXTRA_TITLE, item.title)
            putExtra(ItemReminderReceiver.EXTRA_BODY, item.body)
            putExtra(ItemReminderReceiver.EXTRA_NOTIF_ID, requestCode(item.key))
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode(item.key), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = item.fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    // Stable, collision-resistant codes for dynamic items (kept clear of ReminderType ordinals 0..9).
    private fun requestCode(key: String): Int = 100_000 + (key.hashCode() and 0x7FFFFFFF) % 1_000_000
}
```

- [ ] **Step 3: Register the receiver in the manifest**

In `AndroidManifest.xml` `<application>`, alongside the existing `ReminderReceiver`:
```xml
        <receiver android:name=".reminders.ItemReminderReceiver" android:exported="false" />
```

- [ ] **Step 4: Integrate into `ReminderManager`**

Modify `ReminderManager` to also read meds + appointments and schedule them. Inject `MedicationRepository` and `AppointmentRepository`. After the existing cycle reschedule, add:
```kotlin
        val meds = medicationRepository.observeAll().first().map {
            MedAppointmentScheduler.MedInput(it.id, it.name, it.timeMinutes, it.enabled)
        }
        val appts = appointmentRepository.observeAll().first().map {
            MedAppointmentScheduler.ApptInput(it.id, it.title, it.atEpochMillis, it.leadHours, it.enabled)
        }
        val items = MedAppointmentScheduler.compute(meds, appts, java.time.LocalDateTime.now(), java.time.ZoneId.systemDefault())
        ItemAlarmScheduler(context).scheduleAll(items)
```
Add the two repositories to the `ReminderManager` constructor (`@Inject`), and the needed imports (`com.domina.cycle.data.repository.*`, `com.domina.cycle.domain.reminders.MedAppointmentScheduler`, `kotlinx.coroutines.flow.first`).

- [ ] **Step 5: Build, install, verify a meds/appointment notification fires**

```bash
./gradlew :app:installDebug
adb shell pm grant com.domina.cycle android.permission.POST_NOTIFICATIONS
```
Write a THROWAWAY instrumented test `app/src/androidTest/java/com/domina/cycle/ItemNotifySmokeTest.kt` whose single `@Test` calls
`com.domina.cycle.reminders.Notifier(InstrumentationRegistry.getInstrumentation().targetContext).notifyRaw(com.domina.cycle.reminders.NotificationChannels.MEDS, 123456, "Time for Vitamin 💊", "test")`,
run it via `./gradlew :app:installDebugAndroidTest` + `adb shell am instrument -w -e class com.domina.cycle.ItemNotifySmokeTest com.domina.cycle.test/androidx.test.runner.AndroidJUnitRunner`, confirm with `adb shell dumpsys notification --noredact | grep -i "Vitamin\|meds_appointments"`, then **DELETE** the throwaway test. Reinstall the app afterward (`./gradlew :app:installDebug`).

- [ ] **Step 6: Commit** (excluding the deleted throwaway test)

```bash
git add app/src/main/java/com/domina/cycle/reminders/ app/src/main/AndroidManifest.xml
git commit -m "feat: id-keyed item alarms + meds/appointments channel + ReminderManager integration"
```

---

## Task 4: Medications screen (add / list / delete)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/meds/MedsViewModel.kt`, `MedsScreen.kt`
- Modify: `ui/nav/Destinations.kt`, `AppNav.kt` (route + entry from Settings)

- [ ] **Step 1: Implement `MedsViewModel`**

```kotlin
package com.domina.cycle.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.MedicationEntity
import com.domina.cycle.data.repository.MedicationRepository
import com.domina.cycle.reminders.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedsViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val reminderManager: ReminderManager,
) : ViewModel() {
    val meds: StateFlow<List<MedicationEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, timeMinutes: Int) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.add(name.trim(), timeMinutes); reminderManager.reschedule() }
    }
    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id); reminderManager.reschedule() }
    }
}
```

- [ ] **Step 2: Implement `MedsScreen`**

```kotlin
package com.domina.cycle.ui.meds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MedsScreen(vm: MedsViewModel = hiltViewModel()) {
    val meds by vm.meds.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("9") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Medications", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, label = { Text("Hr") }, modifier = Modifier.width(72.dp))
        }
        Button(onClick = {
            val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 9
            vm.add(name, h * 60); name = ""
        }, enabled = name.isNotBlank()) { Text("Add medication") }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(meds, key = { it.id }) { m ->
                ListItem(
                    headlineContent = { Text(m.name) },
                    supportingContent = { Text("Daily at %02d:%02d".format(m.timeMinutes / 60, m.timeMinutes % 60)) },
                    trailingContent = {
                        IconButton(onClick = { vm.delete(m.id) }) { Icon(Icons.Filled.Delete, "Delete") }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
```

- [ ] **Step 3: Add the route + an entry point**

In `Destinations.kt` add `const val MEDS = "meds"`. In `AppNav.kt` add `composable(Destinations.MEDS) { MedsScreen() }`. Add a navigation entry from the Settings screen (a `TextButton`/`ListItem` "Medications" that calls a passed-in `onOpenMeds` lambda) — wire `SettingsScreen(onOpenMeds = { nav.navigate(Destinations.MEDS) }, onOpenAppointments = { nav.navigate(Destinations.APPOINTMENTS) })` from `AppNav`. (Add the two lambda params to `SettingsScreen`; the Appointments one is used in Task 5.)

- [ ] **Step 4: Build & install**

Run: `./gradlew :app:installDebug` — BUILD SUCCESSFUL; launch and confirm no crash; navigate Settings → Medications, add one, see it listed, delete it.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/ui/meds/ app/src/main/java/com/domina/cycle/ui/nav/ \
        app/src/main/java/com/domina/cycle/ui/settings/SettingsScreen.kt
git commit -m "feat: medications add/list/delete screen with reschedule"
```

---

## Task 5: Appointments screen + full verification

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/appointments/AppointmentsViewModel.kt`, `AppointmentsScreen.kt`
- Modify: `ui/nav/Destinations.kt`, `AppNav.kt`

- [ ] **Step 1: Implement `AppointmentsViewModel`**

```kotlin
package com.domina.cycle.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.AppointmentEntity
import com.domina.cycle.data.repository.AppointmentRepository
import com.domina.cycle.reminders.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val repository: AppointmentRepository,
    private val reminderManager: ReminderManager,
) : ViewModel() {
    val appointments: StateFlow<List<AppointmentEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, atEpochMillis: Long, leadHours: Int) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.add(title.trim(), atEpochMillis, leadHours); reminderManager.reschedule() }
    }
    fun delete(id: Long) { viewModelScope.launch { repository.delete(id); reminderManager.reschedule() } }
}
```

- [ ] **Step 2: Implement `AppointmentsScreen`**

```kotlin
package com.domina.cycle.ui.appointments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun AppointmentsScreen(vm: AppointmentsViewModel = hiltViewModel()) {
    val appts by vm.appointments.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }   // YYYY-MM-DD
    var time by remember { mutableStateOf("10:00") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Appointments", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Row {
            OutlinedTextField(date, { date = it }, label = { Text("Date YYYY-MM-DD") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(time, { time = it }, label = { Text("HH:MM") }, modifier = Modifier.width(110.dp))
        }
        Button(onClick = {
            val parsed = runCatching {
                LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
            if (parsed != null) { vm.add(title, parsed, 24); title = ""; date = "" }
        }, enabled = title.isNotBlank()) { Text("Add appointment (reminds 24h before)") }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(appts, key = { it.id }) { a ->
                val whenText = java.time.Instant.ofEpochMilli(a.atEpochMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDateTime().toString().replace('T', ' ')
                ListItem(
                    headlineContent = { Text(a.title) },
                    supportingContent = { Text(whenText) },
                    trailingContent = { IconButton(onClick = { vm.delete(a.id) }) { Icon(Icons.Filled.Delete, "Delete") } },
                )
                HorizontalDivider()
            }
        }
    }
}
```

- [ ] **Step 3: Add the route**

In `Destinations.kt` add `const val APPOINTMENTS = "appointments"`. In `AppNav.kt` add `composable(Destinations.APPOINTMENTS) { AppointmentsScreen() }` and ensure the Settings `onOpenAppointments` lambda navigates here.

- [ ] **Step 4: Build, run full suites, verify on device**

Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```
Expected: all pass (Phase 1 encryption + the new `MedsAppointmentsDaoTest` instrumented test; all unit tests including `MedAppointmentSchedulerTest`).
On device: Settings → Appointments → add one a couple days out → it appears; delete works. Settings → Medications still works. Confirm no crash in `adb logcat -d`.

- [ ] **Step 5: Reinstall so the app stays, then commit**

```bash
./gradlew :app:installDebug
adb shell pm list packages | grep domina   # confirm present
git add app/src/main/java/com/domina/cycle/ui/appointments/ app/src/main/java/com/domina/cycle/ui/nav/
git commit -m "feat: appointments add/list/delete screen with 24h-before reminders"
```

---

## Phase 3b Definition of Done

- `MedAppointmentScheduler` is pure Kotlin with passing unit tests; meds/appointments DAO CRUD passes instrumented tests.
- The v1→v2 migration is non-destructive — verified that existing on-device cycle data survives the upgrade.
- Meds remind daily at their time; appointments remind 24h before; both reschedule on add/delete, on boot, and via the daily worker (they flow through `ReminderManager`).
- A meds/appointment notification provably fires on the device.
- Full unit + instrumented suites green; app installed at the end. Still **no `INTERNET` permission**.

## Deferred (later)
- Editing existing meds/appointments (currently add + delete); per-item custom lead time UI; weekly/specific-day meds; nicer date/time pickers; tapping a notification to open the item.
```
