# Phase 6a: Encrypted Backup & Restore — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user export all her on-device health data to a single **passphrase-encrypted file** she saves wherever she wants (Drive, email-to-self, SD), and restore it on any device — so years of data survive a lost/broken/replaced phone, with zero cloud involvement.

**Architecture:** A pure-Kotlin `BackupCodec` serializes a generic table map to a Base64-delimited string (bulletproof, dependency-free, JVM-tested). A pure-Kotlin `BackupCrypto` seals it with **PBKDF2 + AES-GCM derived from the user's passphrase** (portable across devices; JVM-tested). `BackupManager` (Android) maps Room entities ↔ rows and reads/writes plain `OutputStream`/`InputStream` (so the SAF `Uri` plumbing lives only in the UI). The export is portable because it's re-encrypted with her passphrase, not the device Keystore key.

**Tech Stack:** Kotlin, javax.crypto, java.util.Base64, Room, Hilt, Compose + Storage Access Framework. No new third-party dependencies. Still **no `INTERNET` permission** (the user chooses where the file goes).

> Tasks 1–2 are pure-JVM unit-tested. Task 3 has an instrumented round-trip (in-memory DB + byte streams — does NOT wipe device data). Task 4 is UI + a real on-device backup→restore. Avoid `connectedDebugAndroidTest` (it wipes data) except where noted; use `am instrument` for the Task 3 test.

## Builds on (existing, on `main`)
- DAOs: `DayLogDao, CycleEventDao, MedicationDao, AppointmentDao, WeightEntryDao, ChecklistItemDao, KickSessionDao, ContractionDao` + their entities.
- `di/DatabaseModule.kt` provides all DAOs; `security/DatabaseKeyProvider.kt` (Keystore pattern, for reference).
- `ui/settings/{SettingsScreen,SettingsViewModel}.kt`.

## New file structure
```
app/src/main/java/com/domina/cycle/
  backup/
    BackupCodec.kt          # pure: Map<String,List<List<String>>> <-> String
    BackupCrypto.kt         # pure: PBKDF2 + AES-GCM seal/open with passphrase
    BackupManager.kt        # Android: entities <-> rows, stream read/write, restore
  ui/settings/SettingsScreen.kt / SettingsViewModel.kt   # MODIFY: backup/restore UI
  data/db/*Dao.kt           # MODIFY: add suspend getAll()/clearAll() where missing
app/src/test/java/com/domina/cycle/backup/
  BackupCodecTest.kt, BackupCryptoTest.kt
app/src/androidTest/java/com/domina/cycle/backup/BackupManagerTest.kt
```

---

## Task 1: BackupCodec (pure)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/backup/BackupCodec.kt`
- Test: `app/src/test/java/com/domina/cycle/backup/BackupCodecTest.kt`

Format: line 1 = `DOMINA-BACKUP-1`. Each table: a line `#<table>` then one line per row; each row is its fields Base64-encoded (URL-safe, no padding) joined by `|`. Empty field lists and special characters round-trip safely (Base64 avoids delimiter collisions).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupCodecTest {
    @Test fun roundTripsTablesWithSpecialCharacters() {
        val data = linkedMapOf(
            "day_logs" to listOf(
                listOf("2026-05-01", "HAPPY", "crampsheadache"),
                listOf("2026-05-02", "", "note with | pipe and \n newline"),
            ),
            "medications" to listOf(listOf("1", "Prenatal vitamin", "540")),
            "empty_table" to emptyList(),
        )
        val encoded = BackupCodec.encode(data)
        val decoded = BackupCodec.decode(encoded)
        assertThat(decoded).isEqualTo(data)
    }

    @Test fun rejectsUnknownHeader() {
        try { BackupCodec.decode("NOT-A-BACKUP\n"); assertThat(false).isTrue() }
        catch (e: IllegalArgumentException) { assertThat(e).hasMessageThat().contains("header") }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*BackupCodecTest*"`
Expected: FAIL — unresolved `BackupCodec`.

- [ ] **Step 3: Implement `BackupCodec`**

```kotlin
package com.domina.cycle.backup

import java.util.Base64

object BackupCodec {
    private const val HEADER = "DOMINA-BACKUP-1"
    private val enc = Base64.getUrlEncoder().withoutPadding()
    private val dec = Base64.getUrlDecoder()

    fun encode(tables: Map<String, List<List<String>>>): String {
        val sb = StringBuilder(HEADER).append('\n')
        tables.forEach { (table, rows) ->
            sb.append('#').append(table).append('\n')
            rows.forEach { row ->
                sb.append(row.joinToString("|") { enc.encodeToString(it.toByteArray(Charsets.UTF_8)) }).append('\n')
            }
        }
        return sb.toString()
    }

    fun decode(text: String): Map<String, List<List<String>>> {
        val lines = text.split('\n')
        require(lines.isNotEmpty() && lines[0] == HEADER) { "Unrecognized backup header" }
        val out = LinkedHashMap<String, MutableList<List<String>>>()
        var current: MutableList<List<String>>? = null
        for (i in 1 until lines.size) {
            val line = lines[i]
            when {
                line.isEmpty() -> {} // trailing/blank lines
                line.startsWith('#') -> { current = mutableListOf(); out[line.substring(1)] = current }
                else -> current?.add(line.split("|").map { String(dec.decode(it), Charsets.UTF_8) })
            }
        }
        return out
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*BackupCodecTest*"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/backup/BackupCodec.kt \
        app/src/test/java/com/domina/cycle/backup/BackupCodecTest.kt
git commit -m "feat: dependency-free Base64 backup codec"
```

---

## Task 2: BackupCrypto (pure)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/backup/BackupCrypto.kt`
- Test: `app/src/test/java/com/domina/cycle/backup/BackupCryptoTest.kt`

Format of sealed bytes: `[16-byte salt][12-byte IV][GCM ciphertext]`. Key = PBKDF2WithHmacSHA256(passphrase, salt, 120k, 256). Wrong passphrase → decryption throws.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupCryptoTest {
    @Test fun sealThenOpenRoundTrips() {
        val plaintext = "DOMINA-BACKUP-1\n#day_logs\nrow".toByteArray()
        val sealed = BackupCrypto.seal(plaintext, "correct horse".toCharArray())
        val opened = BackupCrypto.open(sealed, "correct horse".toCharArray())
        assertThat(String(opened)).isEqualTo(String(plaintext))
    }

    @Test fun wrongPassphraseFails() {
        val sealed = BackupCrypto.seal("secret".toByteArray(), "right".toCharArray())
        try { BackupCrypto.open(sealed, "wrong".toCharArray()); assertThat(false).isTrue() }
        catch (e: Exception) { assertThat(true).isTrue() } // AEADBadTagException or similar
    }

    @Test fun differentSaltsProduceDifferentCiphertext() {
        val a = BackupCrypto.seal("x".toByteArray(), "p".toCharArray())
        val b = BackupCrypto.seal("x".toByteArray(), "p".toCharArray())
        assertThat(a.toList()).isNotEqualTo(b.toList())
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*BackupCryptoTest*"`
Expected: FAIL — unresolved `BackupCrypto`.

- [ ] **Step 3: Implement `BackupCrypto`**

```kotlin
package com.domina.cycle.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128

    fun seal(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext)
        return salt + iv + ct
    }

    fun open(sealed: ByteArray, passphrase: CharArray): ByteArray {
        val salt = sealed.copyOfRange(0, SALT_LEN)
        val iv = sealed.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val ct = sealed.copyOfRange(SALT_LEN + IV_LEN, sealed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*BackupCryptoTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/backup/BackupCrypto.kt \
        app/src/test/java/com/domina/cycle/backup/BackupCryptoTest.kt
git commit -m "feat: passphrase-based AES-GCM backup encryption"
```

---

## Task 3: BackupManager + DAO getAll/clearAll + instrumented round-trip

**Files:**
- Modify the DAOs to add `suspend fun getAll(): List<Entity>` and `suspend fun clearAll()` where missing (DayLog, CycleEvent, Medication, Appointment, WeightEntry, ChecklistItem, KickSession, Contraction).
- Create: `app/src/main/java/com/domina/cycle/backup/BackupManager.kt`
- Test: `app/src/androidTest/java/com/domina/cycle/backup/BackupManagerTest.kt`

- [ ] **Step 1: Add the DAO methods**

For each DAO add (matching table name), e.g. in `DayLogDao`:
```kotlin
    @Query("SELECT * FROM day_logs") suspend fun getAll(): List<DayLogEntity>
    @Query("DELETE FROM day_logs") suspend fun clearAll()
```
Do the same for: `cycle_events` (CycleEventDao), `medications` (MedicationDao), `appointments` (AppointmentDao), `weight_entries` (WeightEntryDao), `checklist_items` (ChecklistItemDao), `kick_sessions` (KickSessionDao), `contractions` (ContractionDao). (ContractionDao already has `clear()`; add `getAll()` and a `clearAll()` alias or reuse `clear()`.)

- [ ] **Step 2: Write the failing instrumented test (in-memory DB + byte streams)**

```kotlin
package com.domina.cycle.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.AppDatabase
import com.domina.cycle.data.db.entity.DayLogEntity
import com.domina.cycle.data.db.entity.MedicationEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BackupManagerTest {
    private lateinit var db: AppDatabase
    private lateinit var manager: BackupManager
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        manager = BackupManager(db)
    }
    @After fun tearDown() = db.close()

    @Test fun exportThenWipeThenImportRestoresData(): Unit = runBlocking {
        db.dayLogDao().upsert(DayLogEntity(date = "2026-05-01", mood = "HAPPY", symptoms = listOf("cramps")))
        db.medicationDao().insert(MedicationEntity(name = "Prenatal vitamin", timeMinutes = 540, enabled = true))

        val out = ByteArrayOutputStream()
        manager.export(out, "pass1234".toCharArray())

        // wipe
        db.dayLogDao().clearAll(); db.medicationDao().clearAll()
        assertThat(db.dayLogDao().getAll()).isEmpty()

        manager.import(ByteArrayInputStream(out.toByteArray()), "pass1234".toCharArray())
        assertThat(db.dayLogDao().getAll().single().mood).isEqualTo("HAPPY")
        assertThat(db.dayLogDao().getAll().single().symptoms).containsExactly("cramps")
        assertThat(db.medicationDao().getAll().single().name).isEqualTo("Prenatal vitamin")
    }

    @Test(expected = Exception::class)
    fun importWithWrongPassphraseThrows(): Unit = runBlocking {
        val out = ByteArrayOutputStream()
        manager.export(out, "right".toCharArray())
        manager.import(ByteArrayInputStream(out.toByteArray()), "wrong".toCharArray())
    }
}
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.domina.cycle.backup.BackupManagerTest` — wait, this WIPES the device. Instead run via am instrument after installing the test APK:
`./gradlew :app:installDebug :app:installDebugAndroidTest` then `adb shell am instrument -w -e class com.domina.cycle.backup.BackupManagerTest com.domina.cycle.test/androidx.test.runner.AndroidJUnitRunner`
Expected: FAIL — unresolved `BackupManager`.

- [ ] **Step 4: Implement `BackupManager`**

```kotlin
package com.domina.cycle.backup

import com.domina.cycle.data.db.AppDatabase
import com.domina.cycle.data.db.entity.*
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/** Gathers all Room data, seals it with a passphrase, and restores it. Streams come from SAF Uris in the UI. */
class BackupManager @Inject constructor(private val db: AppDatabase) {

    suspend fun export(out: OutputStream, passphrase: CharArray) {
        val tables = linkedMapOf(
            "day_logs" to db.dayLogDao().getAll().map {
                listOf(it.date, it.mood ?: "", it.energy ?: "", it.flow, it.symptoms.joinToString(""),
                    it.bbt?.toString() ?: "", it.cervicalMucus ?: "", it.lhResult, it.libido?.toString() ?: "",
                    it.sleepHours?.toString() ?: "", it.weight?.toString() ?: "", it.note)
            },
            "cycle_events" to db.cycleEventDao().getAll().map { listOf(it.id.toString(), it.date, it.type) },
            "medications" to db.medicationDao().getAll().map { listOf(it.id.toString(), it.name, it.timeMinutes.toString(), it.enabled.toString()) },
            "appointments" to db.appointmentDao().getAll().map { listOf(it.id.toString(), it.title, it.atEpochMillis.toString(), it.note, it.leadHours.toString(), it.enabled.toString()) },
            "weight_entries" to db.weightEntryDao().getAll().map { listOf(it.id.toString(), it.dateEpochDay.toString(), it.weightKg.toString()) },
            "checklist_items" to db.checklistItemDao().getAll().map { listOf(it.id.toString(), it.category, it.text, it.checked.toString(), it.sortOrder.toString()) },
            "kick_sessions" to db.kickSessionDao().getAll().map { listOf(it.id.toString(), it.startMillis.toString(), it.endMillis.toString(), it.count.toString()) },
            "contractions" to db.contractionDao().getAll().map { listOf(it.id.toString(), it.startMillis.toString(), it.endMillis.toString()) },
        )
        val sealed = BackupCrypto.seal(BackupCodec.encode(tables).toByteArray(Charsets.UTF_8), passphrase)
        out.use { it.write(sealed) }
    }

    suspend fun import(input: InputStream, passphrase: CharArray) {
        val sealed = input.use { it.readBytes() }
        val tables = BackupCodec.decode(String(BackupCrypto.open(sealed, passphrase), Charsets.UTF_8))
        // Replace all data with the backup's contents.
        db.dayLogDao().clearAll(); db.cycleEventDao().clearAll(); db.medicationDao().clearAll()
        db.appointmentDao().clearAll(); db.weightEntryDao().clearAll(); db.checklistItemDao().clearAll()
        db.kickSessionDao().clearAll(); db.contractionDao().clearAll()

        tables["day_logs"]?.forEach { r ->
            db.dayLogDao().upsert(DayLogEntity(
                date = r[0], mood = r[1].ifEmpty { null }, energy = r[2].ifEmpty { null }, flow = r[3],
                symptoms = if (r[4].isEmpty()) emptyList() else r[4].split(""),
                bbt = r[5].toDoubleOrNull(), cervicalMucus = r[6].ifEmpty { null }, lhResult = r[7],
                libido = r[8].toIntOrNull(), sleepHours = r[9].toDoubleOrNull(), weight = r[10].toDoubleOrNull(), note = r[11]))
        }
        tables["cycle_events"]?.forEach { r -> db.cycleEventDao().insert(CycleEventEntity(date = r[1], type = r[2])) }
        tables["medications"]?.forEach { r -> db.medicationDao().insert(MedicationEntity(name = r[1], timeMinutes = r[2].toInt(), enabled = r[3].toBoolean())) }
        tables["appointments"]?.forEach { r -> db.appointmentDao().insert(AppointmentEntity(title = r[1], atEpochMillis = r[2].toLong(), note = r[3], leadHours = r[4].toInt(), enabled = r[5].toBoolean())) }
        tables["weight_entries"]?.forEach { r -> db.weightEntryDao().insert(WeightEntryEntity(dateEpochDay = r[1].toLong(), weightKg = r[2].toDouble())) }
        tables["checklist_items"]?.forEach { r -> db.checklistItemDao().insert(ChecklistItemEntity(category = r[1], text = r[2], checked = r[3].toBoolean(), sortOrder = r[4].toInt())) }
        tables["kick_sessions"]?.forEach { r -> db.kickSessionDao().insert(KickSessionEntity(startMillis = r[1].toLong(), endMillis = r[2].toLong(), count = r[3].toInt())) }
        tables["contractions"]?.forEach { r -> db.contractionDao().insert(ContractionEntity(startMillis = r[1].toLong(), endMillis = r[2].toLong())) }
    }
}
```

> Note `CycleEventDao` needs an `insert` returning Long (it exists) and a `getAll()`/`clearAll()` (add in Step 1). Confirm every DAO referenced here has `getAll()` and `clearAll()`.

- [ ] **Step 5: Run the instrumented test (via am instrument — no wipe) to verify it passes**

Run: `./gradlew :app:installDebug :app:installDebugAndroidTest` then
`adb shell am instrument -w -e class com.domina.cycle.backup.BackupManagerTest com.domina.cycle.test/androidx.test.runner.AndroidJUnitRunner`
Expected: `OK (2 tests)`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/domina/cycle/data/db/ app/src/main/java/com/domina/cycle/backup/BackupManager.kt \
        app/src/androidTest/java/com/domina/cycle/backup/BackupManagerTest.kt
git commit -m "feat: BackupManager export/import with DAO getAll/clearAll"
```

---

## Task 4: Settings UI — backup & restore with passphrase + SAF

**Files:**
- Modify: `app/src/main/java/com/domina/cycle/ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt`

- [ ] **Step 1: Add backup/restore to `SettingsViewModel`**

Inject `BackupManager` and `@ApplicationContext context`. Add functions that take a `Uri` + passphrase and run on a coroutine, exposing a simple result message `StateFlow<String?>`:
```kotlin
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun clearMessage() { _message.value = null }

    fun backupTo(uri: android.net.Uri, passphrase: String) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { backupManager.export(it, passphrase.toCharArray()) }
            }.onSuccess { _message.value = "Backup saved 💛" }
                .onFailure { _message.value = "Backup failed: ${it.message}" }
        }
    }
    fun restoreFrom(uri: android.net.Uri, passphrase: String) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)!!.use { backupManager.import(it, passphrase.toCharArray()) }
            }.onSuccess { _message.value = "Restored 💛 Reopen the app to see everything." }
                .onFailure { _message.value = "Restore failed — check your passphrase." }
        }
    }
```
(Add imports: `dagger.hilt.android.qualifiers.ApplicationContext`, `android.content.Context`, `com.domina.cycle.backup.BackupManager`, flow/launch.)

- [ ] **Step 2: Add the Backup/Restore section to `SettingsScreen`**

Add a "Backup & restore" section with two buttons that launch SAF and a passphrase dialog. Use:
```kotlin
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var mode by remember { mutableStateOf("") } // "backup" or "restore"
    var passphrase by remember { mutableStateOf("") }
    val message by vm.message.collectAsStateWithLifecycle()

    val createDoc = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) { pendingUri = uri; mode = "backup" } }
    val openDoc = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) { pendingUri = uri; mode = "restore" } }

    // ... in the column:
    Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
    Text("Your data stays on your phone. A backup is an encrypted file you save wherever you like.",
        style = MaterialTheme.typography.bodySmall)
    Button(onClick = { createDoc.launch("domina-backup.dom") }) { Text("Back up (encrypted)") }
    OutlinedButton(onClick = { openDoc.launch(arrayOf("application/octet-stream", "*/*")) }) { Text("Restore from backup") }

    if (pendingUri != null) {
        AlertDialog(
            onDismissRequest = { pendingUri = null; passphrase = "" },
            title = { Text(if (mode == "backup") "Choose a passphrase" else "Enter your passphrase") },
            text = {
                OutlinedTextField(passphrase, { passphrase = it }, label = { Text("Passphrase") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            },
            confirmButton = {
                TextButton(enabled = passphrase.length >= 4, onClick = {
                    val u = pendingUri!!; val p = passphrase
                    if (mode == "backup") vm.backupTo(u, p) else vm.restoreFrom(u, p)
                    pendingUri = null; passphrase = ""
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pendingUri = null; passphrase = "" }) { Text("Cancel") } },
        )
    }
    message?.let { msg ->
        LaunchedEffect(msg) { /* could show a snackbar */ }
        Text(msg, style = MaterialTheme.typography.bodyMedium)
    }
```
Add imports: `androidx.activity.compose.rememberLauncherForActivityResult`, `androidx.compose.runtime.*` (remember/mutableStateOf/getValue/setValue), `androidx.lifecycle.compose.collectAsStateWithLifecycle`.

- [ ] **Step 3: Build & install**

Run: `./gradlew :app:installDebug` → BUILD SUCCESSFUL, launch, `adb logcat -d` shows no crash.

- [ ] **Step 4: Real on-device round trip (manual, non-destructive verification)**

On device: Settings → "Back up (encrypted)" → choose a location & name → enter a passphrase → "Backup saved". Then "Restore from backup" → pick that file → enter the same passphrase → "Restored". Confirm the app data is intact. (This exercises the real SAF + crypto path on the user's actual data.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/ui/settings/
git commit -m "feat: Settings backup & restore (passphrase + Storage Access Framework)"
```

---

## Phase 6a Definition of Done

- `BackupCodec` and `BackupCrypto` are pure Kotlin with passing unit tests; `BackupManager` export→wipe→import round-trip passes instrumented (incl. wrong-passphrase failure).
- From Settings, the user can export an encrypted backup file to any location she chooses and restore it with her passphrase; restore replaces all data.
- The backup is portable (passphrase-encrypted, not tied to the device Keystore), so it restores on a new phone.
- Full unit suite green; app installed at the end. Still **no `INTERNET` permission**; no new dependencies.

## Deferred (later 6x sub-phases)
- 6b: doctor-report PDF export · 6c: custom + discreet app icon · 6d: home-screen widget (Glance).
- Backing up DataStore settings (theme/mode/due date/reminder prefs); auto periodic local backups; restore-merge (vs replace).
```
