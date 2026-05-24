# Phase 4b: Pregnancy Tools — Kick Counter, Contraction Timer, Weight, Checklists — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add four hands-on pregnancy tools reachable from the pregnancy dashboard: a kick counter, a contraction timer (with average duration/interval), pregnancy weight logging, and editable hospital-bag & birth-plan checklists.

**Architecture:** Four new encrypted Room tables via a non-destructive Migration(2→3). The one piece with real logic — contraction stats (avg duration & interval) — is pure Kotlin and unit-tested. Each tool is a small ViewModel + Compose screen; they're reached via new nav routes linked from the pregnancy dashboard. Still no networking — **no `INTERNET` permission.**

**Tech Stack:** Kotlin, Room 2.7.0 + KSP, java.time, Jetpack Compose, Hilt. Builds on Phases 1–4a (on `main`, AppDatabase version 2).

> Task 2 is pure-JVM unit-tested. Tasks 1, 3–6 need the connected Galaxy S23. **Do NOT run `connectedDebugAndroidTest` except where this plan explicitly says (Task 6 final verify)** — it uninstalls the app and erases data. Use `installDebug` otherwise. The device holds real data — the migration must be non-destructive.

## Builds on (existing, on `main`)
- `data/db/AppDatabase.kt` (version 2; entities DayLog, CycleEvent, Medication, Appointment), `data/db/Migrations.kt` (has `MIGRATION_1_2`), `di/DatabaseModule.kt`, `di/RepositoryModule.kt`.
- `domain/pregnancy/...`, `ui/today/PregnancyDashboard.kt` (pregnancy-mode content), `ui/nav/{Destinations,AppNav}.kt`.

## New file structure
```
app/src/main/java/com/domina/cycle/
  data/db/entity/{KickSessionEntity,ContractionEntity,WeightEntryEntity,ChecklistItemEntity}.kt
  data/db/{KickSessionDao,ContractionDao,WeightEntryDao,ChecklistItemDao}.kt
  data/db/Migrations.kt                 # MODIFY: add MIGRATION_2_3
  data/repository/{KickRepository,ContractionRepository,WeightRepository,ChecklistRepository}.kt (+impls)
  domain/pregnancy/ContractionStats.kt  # pure
  domain/pregnancy/ChecklistDefaults.kt # bundled default bag/birth-plan items
  ui/tools/kick/{KickViewModel,KickCounterScreen}.kt
  ui/tools/contractions/{ContractionsViewModel,ContractionTimerScreen}.kt
  ui/tools/weight/{WeightViewModel,WeightScreen}.kt
  ui/tools/checklist/{ChecklistViewModel,ChecklistScreen}.kt
  ui/today/PregnancyDashboard.kt        # MODIFY: add tool links
  ui/nav/{Destinations,AppNav}.kt       # MODIFY: routes
app/src/test/java/com/domina/cycle/domain/pregnancy/ContractionStatsTest.kt
app/src/androidTest/java/com/domina/cycle/data/db/PregnancyToolsDaoTest.kt
```

---

## Task 1: Entities, DAOs, Migration(2→3), repositories

**Files:**
- Create the four entities, four DAOs; modify `Migrations.kt`, `AppDatabase.kt` (v3), `DatabaseModule.kt`, `RepositoryModule.kt`; create four repositories.
- Test: `app/src/androidTest/java/com/domina/cycle/data/db/PregnancyToolsDaoTest.kt`

- [ ] **Step 1: Write the failing instrumented test (in-memory CRUD)**

```kotlin
package com.domina.cycle.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.entity.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PregnancyToolsDaoTest {
    private lateinit var db: AppDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
    }
    @After fun tearDown() = db.close()

    @Test fun kickAndContractionAndWeightAndChecklistCrud(): Unit = runBlocking {
        db.kickSessionDao().insert(KickSessionEntity(startMillis = 1, endMillis = 2, count = 10))
        assertThat(db.kickSessionDao().observeAll().first()).hasSize(1)

        db.contractionDao().insert(ContractionEntity(startMillis = 100, endMillis = 145))
        assertThat(db.contractionDao().observeAll().first().single().endMillis).isEqualTo(145)

        db.weightEntryDao().insert(WeightEntryEntity(dateEpochDay = 20000, weightKg = 65.5))
        assertThat(db.weightEntryDao().observeAll().first().single().weightKg).isEqualTo(65.5)

        val id = db.checklistItemDao().insert(
            ChecklistItemEntity(category = "BAG", text = "Phone charger", checked = false, sortOrder = 0))
        db.checklistItemDao().setChecked(id, true)
        assertThat(db.checklistItemDao().observeByCategory("BAG").first().single().checked).isTrue()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.domina.cycle.data.db.PregnancyToolsDaoTest`
Expected: FAIL — unresolved entities.

- [ ] **Step 3: Implement the four entities**

`entity/KickSessionEntity.kt`:
```kotlin
package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kick_sessions")
data class KickSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long,
    val count: Int,
)
```
`entity/ContractionEntity.kt`:
```kotlin
package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contractions")
data class ContractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long,
)
```
`entity/WeightEntryEntity.kt`:
```kotlin
package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val weightKg: Double,
)
```
`entity/ChecklistItemEntity.kt`:
```kotlin
package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,   // "BAG" or "BIRTH_PLAN"
    val text: String,
    val checked: Boolean,
    val sortOrder: Int,
)
```

- [ ] **Step 4: Implement the four DAOs**

`KickSessionDao.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.KickSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KickSessionDao {
    @Insert suspend fun insert(e: KickSessionEntity): Long
    @Query("SELECT * FROM kick_sessions ORDER BY startMillis DESC") fun observeAll(): Flow<List<KickSessionEntity>>
    @Query("DELETE FROM kick_sessions WHERE id = :id") suspend fun deleteById(id: Long)
}
```
`ContractionDao.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.ContractionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractionDao {
    @Insert suspend fun insert(e: ContractionEntity): Long
    @Query("SELECT * FROM contractions ORDER BY startMillis DESC") fun observeAll(): Flow<List<ContractionEntity>>
    @Query("DELETE FROM contractions") suspend fun clear()
}
```
`WeightEntryDao.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {
    @Insert suspend fun insert(e: WeightEntryEntity): Long
    @Query("SELECT * FROM weight_entries ORDER BY dateEpochDay") fun observeAll(): Flow<List<WeightEntryEntity>>
    @Query("DELETE FROM weight_entries WHERE id = :id") suspend fun deleteById(id: Long)
}
```
`ChecklistItemDao.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.*
import com.domina.cycle.data.db.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {
    @Insert suspend fun insert(e: ChecklistItemEntity): Long
    @Insert suspend fun insertAll(items: List<ChecklistItemEntity>)
    @Query("SELECT * FROM checklist_items WHERE category = :category ORDER BY sortOrder, id")
    fun observeByCategory(category: String): Flow<List<ChecklistItemEntity>>
    @Query("UPDATE checklist_items SET checked = :checked WHERE id = :id") suspend fun setChecked(id: Long, checked: Boolean)
    @Query("DELETE FROM checklist_items WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("SELECT COUNT(*) FROM checklist_items WHERE category = :category") suspend fun countByCategory(category: String): Int
}
```

- [ ] **Step 5: Add `MIGRATION_2_3`, bump DB, wire DI**

Append to `Migrations.kt`:
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `kick_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startMillis` INTEGER NOT NULL, `endMillis` INTEGER NOT NULL, `count` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `contractions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startMillis` INTEGER NOT NULL, `endMillis` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `weight_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `weightKg` REAL NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `checklist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `text` TEXT NOT NULL, `checked` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)")
    }
}
```
In `AppDatabase.kt`: bump `version = 3`, add the four entities to `entities = [...]`, add the four DAO accessors (`kickSessionDao()`, `contractionDao()`, `weightEntryDao()`, `checklistItemDao()`).
In `DatabaseModule.kt`: add `.addMigrations(MIGRATION_1_2, MIGRATION_2_3)` (replace the existing single-migration call) and add `@Provides` for the four new DAOs.

> If Room reports a schema mismatch, copy the exact expected `CREATE TABLE` from the error. `Double` → `REAL`, `Long`/`Int`/`Boolean` → `INTEGER`.

- [ ] **Step 6: Implement the four repositories + bind them**

Create thin repositories (interface + `@Inject` Room impl) for each DAO. `KickRepository`(observeAll, add(start,end,count), delete(id)); `ContractionRepository`(observeAll, add(start,end), clear); `WeightRepository`(observeAll, add(dateEpochDay,kg), delete(id)); `ChecklistRepository`(observeByCategory(cat), setChecked(id,checked), add(cat,text,sortOrder), delete(id), countByCategory(cat)). Example:
```kotlin
package com.domina.cycle.data.repository

import com.domina.cycle.data.db.ContractionDao
import com.domina.cycle.data.db.entity.ContractionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ContractionRepository {
    fun observeAll(): Flow<List<ContractionEntity>>
    suspend fun add(startMillis: Long, endMillis: Long): Long
    suspend fun clear()
}
class RoomContractionRepository @Inject constructor(private val dao: ContractionDao) : ContractionRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun add(startMillis: Long, endMillis: Long) = dao.insert(ContractionEntity(startMillis = startMillis, endMillis = endMillis))
    override suspend fun clear() = dao.clear()
}
```
(Follow the same pattern for Kick, Weight, Checklist.) Append `@Binds @Singleton` for all four to `RepositoryModule.kt`.

- [ ] **Step 7: Verify CRUD test passes, then migration-safety on device**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.domina.cycle.data.db.PregnancyToolsDaoTest` → PASS (1 test).
Then migration safety over existing data: BEFORE the connected test wipes the device, OR after reinstalling — install v3 over the device's current v2 DB and confirm no crash:
`./gradlew :app:installDebug` then `adb shell am start -n com.domina.cycle/.MainActivity` then `adb logcat -d | grep -iE "migrat|IllegalStateException|Room cannot|FATAL" | grep -i "domina\|room\|migrat" | tail`. Expected: no Room/migration error from the app. (Note: the connectedDebugAndroidTest uninstalls+wipes; the controller re-seeds at the end. The key check is that v3 opens the existing v2 DB without a migration crash — do this install-over-data check while the device still has the v2 DB, i.e., before any connectedDebugAndroidTest run if possible; otherwise rely on MIGRATION_2_3 being additive `CREATE TABLE IF NOT EXISTS`.)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/domina/cycle/data/ app/src/androidTest/java/com/domina/cycle/data/db/PregnancyToolsDaoTest.kt
git commit -m "feat: pregnancy-tools tables (kick/contraction/weight/checklist) + migration v3"
```

---

## Task 2: ContractionStats (pure)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/pregnancy/ContractionStats.kt`
- Test: `app/src/test/java/com/domina/cycle/domain/pregnancy/ContractionStatsTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.domain.pregnancy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContractionStatsTest {
    // each pair is (startMillis, endMillis)
    private fun c(start: Long, end: Long) = ContractionStats.Contraction(start, end)

    @Test fun emptyHasZeroStats() {
        val s = ContractionStats.compute(emptyList())
        assertThat(s.count).isEqualTo(0)
        assertThat(s.averageDurationSec).isEqualTo(0)
        assertThat(s.averageIntervalSec).isEqualTo(0)
    }

    @Test fun computesAverageDurationAndInterval() {
        // contractions starting at 0s, 300s, 600s; each 60s long
        val list = listOf(c(0, 60_000), c(300_000, 360_000), c(600_000, 660_000))
        val s = ContractionStats.compute(list)
        assertThat(s.count).isEqualTo(3)
        assertThat(s.averageDurationSec).isEqualTo(60)        // each 60s
        assertThat(s.averageIntervalSec).isEqualTo(300)       // 5 minutes apart
    }

    @Test fun handlesUnsortedInput() {
        val list = listOf(c(600_000, 660_000), c(0, 60_000), c(300_000, 360_000))
        assertThat(ContractionStats.compute(list).averageIntervalSec).isEqualTo(300)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ContractionStatsTest*"`
Expected: FAIL — unresolved `ContractionStats`.

- [ ] **Step 3: Implement `ContractionStats`**

```kotlin
package com.domina.cycle.domain.pregnancy

object ContractionStats {
    data class Contraction(val startMillis: Long, val endMillis: Long)
    data class Stats(val count: Int, val averageDurationSec: Int, val averageIntervalSec: Int)

    fun compute(contractions: List<Contraction>): Stats {
        if (contractions.isEmpty()) return Stats(0, 0, 0)
        val sorted = contractions.sortedBy { it.startMillis }
        val avgDuration = sorted.map { (it.endMillis - it.startMillis) / 1000.0 }.average()
        val intervals = sorted.map { it.startMillis }
            .zipWithNext { a, b -> (b - a) / 1000.0 }
        val avgInterval = if (intervals.isEmpty()) 0.0 else intervals.average()
        return Stats(sorted.size, Math.round(avgDuration).toInt(), Math.round(avgInterval).toInt())
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*ContractionStatsTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/pregnancy/ContractionStats.kt \
        app/src/test/java/com/domina/cycle/domain/pregnancy/ContractionStatsTest.kt
git commit -m "feat: pure contraction stats (avg duration & interval)"
```

---

## Task 3: Kick counter

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/tools/kick/KickViewModel.kt`, `KickCounterScreen.kt`

- [ ] **Step 1: Implement `KickViewModel`**

```kotlin
package com.domina.cycle.ui.tools.kick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.KickSessionEntity
import com.domina.cycle.data.repository.KickRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KickViewModel @Inject constructor(
    private val repository: KickRepository,
) : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()
    private var startMillis: Long = 0L

    val sessions: StateFlow<List<KickSessionEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun kick() {
        if (_count.value == 0) startMillis = System.currentTimeMillis()
        _count.value += 1
    }
    fun save() {
        if (_count.value == 0) return
        val s = startMillis; val c = _count.value
        viewModelScope.launch { repository.add(s, System.currentTimeMillis(), c) }
        _count.value = 0
    }
    fun reset() { _count.value = 0 }
}
```

- [ ] **Step 2: Implement `KickCounterScreen`**

```kotlin
package com.domina.cycle.ui.tools.kick

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId

@Composable
fun KickCounterScreen(vm: KickViewModel = hiltViewModel()) {
    val count by vm.count.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Kick counter 👶", style = MaterialTheme.typography.titleLarge)
        Text("Tap each time you feel a kick. Ten in a session is a lovely sign 💛",
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Text("$count", fontSize = 72.sp, fontWeight = FontWeight.Bold)
        Button(onClick = vm::kick, modifier = Modifier.fillMaxWidth().height(72.dp)) { Text("I felt a kick!") }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = vm::reset, modifier = Modifier.weight(1f)) { Text("Reset") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = vm::save, enabled = count > 0, modifier = Modifier.weight(1f)) { Text("Save session") }
        }
        Spacer(Modifier.height(16.dp))
        Text("Past sessions", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(sessions, key = { it.id }) { s ->
                val date = Instant.ofEpochMilli(s.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val mins = ((s.endMillis - s.startMillis) / 60000).coerceAtLeast(0)
                ListItem(headlineContent = { Text("${s.count} kicks") },
                    supportingContent = { Text("$date · ${mins} min") })
                HorizontalDivider()
            }
        }
    }
}
```

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/domina/cycle/ui/tools/kick/
git commit -m "feat: kick counter tool"
```

---

## Task 4: Contraction timer

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/tools/contractions/ContractionsViewModel.kt`, `ContractionTimerScreen.kt`

- [ ] **Step 1: Implement `ContractionsViewModel`**

```kotlin
package com.domina.cycle.ui.tools.contractions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.ContractionEntity
import com.domina.cycle.data.repository.ContractionRepository
import com.domina.cycle.domain.pregnancy.ContractionStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContractionsViewModel @Inject constructor(
    private val repository: ContractionRepository,
) : ViewModel() {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()
    private var startMillis = 0L

    val contractions: StateFlow<List<ContractionEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<ContractionStats.Stats> = contractions
        .map { list -> ContractionStats.compute(list.map { ContractionStats.Contraction(it.startMillis, it.endMillis) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContractionStats.Stats(0, 0, 0))

    fun toggle() {
        if (!_running.value) { startMillis = System.currentTimeMillis(); _running.value = true }
        else {
            val s = startMillis; val e = System.currentTimeMillis()
            viewModelScope.launch { repository.add(s, e) }
            _running.value = false
        }
    }
    fun clearAll() { viewModelScope.launch { repository.clear() } }
}
```

- [ ] **Step 2: Implement `ContractionTimerScreen`**

```kotlin
package com.domina.cycle.ui.tools.contractions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ContractionTimerScreen(vm: ContractionsViewModel = hiltViewModel()) {
    val running by vm.running.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val list by vm.contractions.collectAsStateWithLifecycle()
    val fmt = remember { DateTimeFormatter.ofPattern("MMM d, HH:mm:ss") }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Contraction timer", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = vm::toggle, modifier = Modifier.fillMaxWidth().height(80.dp)) {
            Text(if (running) "Stop contraction" else "Start contraction")
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Averages (${stats.count})", style = MaterialTheme.typography.titleMedium)
                Text("Duration: ${stats.averageDurationSec}s · Apart: ${stats.averageIntervalSec / 60}m ${stats.averageIntervalSec % 60}s")
                Text("Tip: contractions ~5 min apart, ~1 min long, for 1 hour often means it's time to call your provider 💛",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = vm::clearAll) { Text("Clear all") }
        LazyColumn {
            items(list, key = { it.id }) { c ->
                val dur = ((c.endMillis - c.startMillis) / 1000).coerceAtLeast(0)
                val start = Instant.ofEpochMilli(c.startMillis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(fmt)
                ListItem(headlineContent = { Text("${dur}s") }, supportingContent = { Text(start) })
                HorizontalDivider()
            }
        }
    }
}
```

- [ ] **Step 3: Build & commit**

```bash
./gradlew :app:assembleDebug   # BUILD SUCCESSFUL
git add app/src/main/java/com/domina/cycle/ui/tools/contractions/
git commit -m "feat: contraction timer with averages"
```

---

## Task 5: Weight tracking

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/tools/weight/WeightViewModel.kt`, `WeightScreen.kt`

- [ ] **Step 1: Implement `WeightViewModel`**

```kotlin
package com.domina.cycle.ui.tools.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.WeightEntryEntity
import com.domina.cycle.data.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository,
) : ViewModel() {
    val entries: StateFlow<List<WeightEntryEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(kg: Double, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch { repository.add(date.toEpochDay(), kg) }
    }
    fun delete(id: Long) { viewModelScope.launch { repository.delete(id) } }
}
```

- [ ] **Step 2: Implement `WeightScreen`**

```kotlin
package com.domina.cycle.ui.tools.weight

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

@Composable
fun WeightScreen(vm: WeightViewModel = hiltViewModel()) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    var kg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Weight", style = MaterialTheme.typography.titleLarge)
        Row {
            OutlinedTextField(kg, { kg = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Weight (kg)") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = { kg.toDoubleOrNull()?.let { vm.add(it); kg = "" } }, enabled = kg.toDoubleOrNull() != null) {
                Text("Add")
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(entries, key = { it.id }) { e ->
                ListItem(
                    headlineContent = { Text("%.1f kg".format(e.weightKg)) },
                    supportingContent = { Text(LocalDate.ofEpochDay(e.dateEpochDay).toString()) },
                    trailingContent = { IconButton(onClick = { vm.delete(e.id) }) { Icon(Icons.Filled.Delete, "Delete") } },
                )
                HorizontalDivider()
            }
        }
    }
}
```

- [ ] **Step 3: Build & commit**

```bash
./gradlew :app:assembleDebug   # BUILD SUCCESSFUL
git add app/src/main/java/com/domina/cycle/ui/tools/weight/
git commit -m "feat: pregnancy weight tracking"
```

---

## Task 6: Checklists + tool navigation + full verification

**Files:**
- Create: `app/src/main/java/com/domina/cycle/domain/pregnancy/ChecklistDefaults.kt`
- Create: `app/src/main/java/com/domina/cycle/ui/tools/checklist/ChecklistViewModel.kt`, `ChecklistScreen.kt`
- Modify: `ui/today/PregnancyDashboard.kt` (tool links), `ui/nav/Destinations.kt`, `AppNav.kt`

- [ ] **Step 1: Implement `ChecklistDefaults`**

```kotlin
package com.domina.cycle.domain.pregnancy

object ChecklistDefaults {
    const val BAG = "BAG"
    const val BIRTH_PLAN = "BIRTH_PLAN"

    val bag = listOf(
        "ID & insurance / hospital papers", "Phone + charger", "Comfy going-home clothes",
        "Toiletries & lip balm", "Nursing bra & comfy underwear", "Snacks & drinks",
        "Outfit for baby", "Newborn diapers & wipes", "Cozy socks & slippers",
    )
    val birthPlan = listOf(
        "Pain-relief preferences", "Who will be in the room", "Movement & positions during labor",
        "Delayed cord clamping", "Skin-to-skin right after birth", "Feeding plan (breast / bottle)",
        "Photos / videos preferences",
    )
}
```

- [ ] **Step 2: Implement `ChecklistViewModel` (seeds defaults once per category)**

```kotlin
package com.domina.cycle.ui.tools.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.ChecklistItemEntity
import com.domina.cycle.data.repository.ChecklistRepository
import com.domina.cycle.domain.pregnancy.ChecklistDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val repository: ChecklistRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            if (repository.countByCategory(ChecklistDefaults.BAG) == 0) {
                ChecklistDefaults.bag.forEachIndexed { i, t -> repository.add(ChecklistDefaults.BAG, t, i) }
            }
            if (repository.countByCategory(ChecklistDefaults.BIRTH_PLAN) == 0) {
                ChecklistDefaults.birthPlan.forEachIndexed { i, t -> repository.add(ChecklistDefaults.BIRTH_PLAN, t, i) }
            }
        }
    }
    fun items(category: String): StateFlow<List<ChecklistItemEntity>> =
        repository.observeByCategory(category).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggle(id: Long, checked: Boolean) { viewModelScope.launch { repository.setChecked(id, checked) } }
    fun add(category: String, text: String) { if (text.isNotBlank()) viewModelScope.launch { repository.add(category, text.trim(), 999) } }
    fun delete(id: Long) { viewModelScope.launch { repository.delete(id) } }
}
```

- [ ] **Step 3: Implement `ChecklistScreen` (tabbed bag / birth plan)**

```kotlin
package com.domina.cycle.ui.tools.checklist

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
import com.domina.cycle.domain.pregnancy.ChecklistDefaults

@Composable
fun ChecklistScreen(vm: ChecklistViewModel = hiltViewModel()) {
    var tab by remember { mutableStateOf(0) }
    val category = if (tab == 0) ChecklistDefaults.BAG else ChecklistDefaults.BIRTH_PLAN
    val items by vm.items(category).collectAsStateWithLifecycle()
    var newItem by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Hospital bag") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Birth plan") })
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(newItem, { newItem = it }, label = { Text("Add item") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.add(category, newItem); newItem = "" }, enabled = newItem.isNotBlank()) { Text("Add") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(items, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.checked, onCheckedChange = { vm.toggle(item.id, it) })
                    Text(item.text, Modifier.weight(1f))
                    IconButton(onClick = { vm.delete(item.id) }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Add tool links to the pregnancy dashboard + nav routes**

In `Destinations.kt` add: `KICK = "kick"`, `CONTRACTIONS = "contractions"`, `WEIGHT = "weight"`, `CHECKLIST = "checklist"`.
In `PregnancyDashboard.kt`, add a `tools row/section` (a `params` lambda set) — change its signature to `PregnancyDashboard(state, onOpenKick, onOpenContractions, onOpenWeight, onOpenChecklist)` with four `OutlinedButton`s ("👶 Kicks", "⏱️ Contractions", "⚖️ Weight", "✅ Checklists"). Provide default no-op lambdas so existing tests/usages don't break.
In `AppNav.kt`: register the four composables (`KickCounterScreen()`, `ContractionTimerScreen()`, `WeightScreen()`, `ChecklistScreen()`), and `TodayScreen` must forward the navigation lambdas down to `PregnancyDashboard`. Add the four nav lambdas to `TodayScreen`'s signature (default no-op) and pass them through; in `AppNav`'s `TodayScreen(...)` call, wire each to `nav.navigate(Destinations.X)`.

- [ ] **Step 5: Build, full suites, install, verify on device**

Run:
```bash
./gradlew :app:testDebugUnitTest                # all pass (incl. ContractionStatsTest)
./gradlew :app:connectedDebugAndroidTest        # all pass (incl. PregnancyToolsDaoTest); FINAL verify
./gradlew :app:installDebug                      # reinstall so app is present
adb shell pm list packages | grep domina        # confirm com.domina.cycle present
```
On device: Settings → Mode → Pregnancy (+ due date) → Today shows the pregnancy dashboard with the four tool buttons → open each: count a kick & save; start/stop a contraction & see averages; add a weight; check off bag/birth-plan items (defaults appear).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/domina/cycle/domain/pregnancy/ChecklistDefaults.kt \
        app/src/main/java/com/domina/cycle/ui/tools/checklist/ \
        app/src/main/java/com/domina/cycle/ui/today/PregnancyDashboard.kt \
        app/src/main/java/com/domina/cycle/ui/today/TodayScreen.kt \
        app/src/main/java/com/domina/cycle/ui/nav/
git commit -m "feat: hospital-bag & birth-plan checklists + pregnancy tool navigation"
```

---

## Phase 4b Definition of Done

- `ContractionStats` pure logic unit-tested; pregnancy-tools DAO CRUD instrumented-tested.
- Non-destructive Migration(2→3) — existing data survives the upgrade.
- From Pregnancy mode, the dashboard links to all four tools, each working: kick counter (count + save sessions), contraction timer (start/stop + averages + 5-1-1 tip), weight logging (add/list), and hospital-bag/birth-plan checklists (defaults seeded, check/add/delete).
- Full unit + instrumented suites green; app installed at the end. Still **no `INTERNET` permission**.

## Deferred (later)
- Weight & contraction trend charts (Phase 5 with Vico). Reordering checklist items; editing kick/contraction history; richer kick-session goal (count-to-10 timer with auto-stop).
```
