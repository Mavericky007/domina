# Phase 1: Foundation & Core MVP — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A buildable, locked, encrypted Android app where the user can log daily cycle data and browse it on a calendar and a Today dashboard, with three switchable themes — all stored only on the device.

**Architecture:** Single-activity Jetpack Compose app, MVVM + clean layering. Pure-Kotlin domain models and repository *interfaces* let ViewModels be unit-tested with fakes. Persistence is Room (SQLite) encrypted with SQLCipher, whose passphrase is sealed by an Android Keystore AES key. No `INTERNET` permission.

**Tech Stack:** Kotlin 2.1, Jetpack Compose, Hilt, Room + SQLCipher (`net.zetetic:sqlcipher-android`), DataStore, AndroidX Biometric, Coroutines/Flow, JUnit4 + Truth, Robolectric-free (instrumented tests for DB/Keystore).

> Run all Gradle commands from the project root `/Users/jounaid/domina`. The emulator/device must be running for instrumented tests (`connectedDebugAndroidTest`). Unit tests use `testDebugUnitTest`.

---

## File Structure

Package root: `com.domina.cycle`

```
app/
  build.gradle.kts
  src/main/AndroidManifest.xml            # NO internet permission
  src/main/java/com/domina/cycle/
    CycleApp.kt                            # @HiltAndroidApp
    MainActivity.kt                        # single activity, hosts NavHost behind the lock
    security/
      DatabaseKeyProvider.kt               # Keystore-sealed DB passphrase
      PinManager.kt                        # PBKDF2 PIN hash/verify (pure JVM)
      BiometricAuthenticator.kt            # BiometricPrompt wrapper
    data/
      model/                               # pure-Kotlin enums + domain models
        Mood.kt  FlowIntensity.kt  Energy.kt  CervicalMucus.kt  LhResult.kt
        DayLog.kt  CycleEvent.kt
      db/
        entity/DayLogEntity.kt  entity/CycleEventEntity.kt
        Converters.kt
        DayLogDao.kt  CycleEventDao.kt
        AppDatabase.kt
      repository/
        DayLogRepository.kt                # interface
        CycleRepository.kt                 # interface
        RoomDayLogRepository.kt
        RoomCycleRepository.kt
      prefs/
        ThemePreference.kt                 # enum + pure string mapping
        SettingsRepository.kt              # DataStore wrapper
    di/
      DatabaseModule.kt  RepositoryModule.kt
    ui/
      theme/Theme.kt  Color.kt  Type.kt    # 3 ColorSchemes + AppTheme()
      lock/LockScreen.kt  LockViewModel.kt
      nav/AppNav.kt  Destinations.kt
      today/TodayScreen.kt  TodayViewModel.kt
      log/LogScreen.kt  LogViewModel.kt
      calendar/CalendarScreen.kt  CalendarViewModel.kt
  src/test/java/com/domina/cycle/         # JVM unit tests
  src/androidTest/java/com/domina/cycle/  # instrumented tests
gradle/libs.versions.toml
settings.gradle.kts
build.gradle.kts
```

---

## Task 1: Project scaffolding & build config

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/domina/cycle/CycleApp.kt`, `MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Create the Gradle wrapper**

Run from project root:
```bash
gradle wrapper --gradle-version 8.11.1
```
If `gradle` is not installed, download the wrapper jar/properties for 8.11.1 manually. Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/` created.

- [ ] **Step 2: Write `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Domina"
include(":app")
```

- [ ] **Step 3: Write `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
navigation = "2.8.5"
hilt = "2.53.1"
hiltNavCompose = "1.2.0"
room = "2.6.1"
sqlcipher = "4.6.1"
sqlite = "2.4.0"
datastore = "1.1.1"
biometric = "1.2.0-alpha05"
coroutines = "1.9.0"
junit = "4.13.2"
truth = "1.4.4"
androidxJunit = "1.2.1"
coroutinesTest = "1.9.0"

[libraries]
core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-material-icons = { module = "androidx.compose.material:material-icons-extended" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavCompose" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
sqlcipher = { module = "net.zetetic:sqlcipher-android", version.ref = "sqlcipher" }
sqlite = { module = "androidx.sqlite:sqlite", version.ref = "sqlite" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
biometric = { module = "androidx.biometric:biometric-ktx", version.ref = "biometric" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutinesTest" }
androidx-junit = { module = "androidx.test.ext:junit", version.ref = "androidxJunit" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 4: Write root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 5: Write `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.domina.cycle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.domina.cycle"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.sqlite)
    implementation(libs.datastore.preferences)
    implementation(libs.biometric)
    implementation(libs.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(platform(libs.compose.bom))
}
```

- [ ] **Step 6: Write `app/src/main/AndroidManifest.xml` (note: NO internet permission)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.USE_BIOMETRIC" />

    <application
        android:name=".CycleApp"
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Domina">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Domina">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7: Write resources**

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">Domina</string>
</resources>
```

`app/src/main/res/values/themes.xml`:
```xml
<resources>
    <style name="Theme.Domina" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 8: Write `CycleApp.kt`**

```kotlin
package com.domina.cycle

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CycleApp : Application()
```

- [ ] **Step 9: Write a minimal `MainActivity.kt` (replaced in Task 9)**

```kotlin
package com.domina.cycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { Text("Domina is alive") }
            }
        }
    }
}
```

- [ ] **Step 10: Build and install to verify the toolchain**

Run:
```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Then with an emulator running:
```bash
./gradlew :app:installDebug
```
Expected: app installs and launches showing "Domina is alive".

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat: scaffold Compose + Hilt Android project (no internet permission)"
```

---

## Task 2: DatabaseKeyProvider (Keystore-sealed passphrase)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/security/DatabaseKeyProvider.kt`
- Test: `app/src/androidTest/java/com/domina/cycle/security/DatabaseKeyProviderTest.kt`

- [ ] **Step 1: Write the failing instrumented test**

```kotlin
package com.domina.cycle.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseKeyProviderTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun returnsStableNonEmptyPassphraseAcrossInstances() {
        context.getSharedPreferences("secure_db", 0).edit().clear().commit()
        val first = DatabaseKeyProvider(context).getOrCreatePassphrase()
        val second = DatabaseKeyProvider(context).getOrCreatePassphrase()

        assertThat(first).isNotEmpty()
        assertThat(first).isEqualTo(second) // persisted & decrypted identically
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*DatabaseKeyProviderTest*"`
Expected: FAIL — `DatabaseKeyProvider` unresolved.

- [ ] **Step 3: Implement `DatabaseKeyProvider`**

```kotlin
package com.domina.cycle.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Provides the SQLCipher passphrase. A random 32-byte passphrase is generated once,
 * sealed with an AES key held in the Android Keystore, and stored as ciphertext in
 * SharedPreferences. The raw passphrase never leaves the device and is never stored in clear.
 */
class DatabaseKeyProvider(private val context: Context) {

    private val prefs = context.getSharedPreferences("secure_db", Context.MODE_PRIVATE)

    fun getOrCreatePassphrase(): ByteArray {
        prefs.getString(KEY_CIPHERTEXT, null)?.let { stored ->
            val iv = Base64.decode(prefs.getString(KEY_IV, null), Base64.NO_WRAP)
            return decrypt(Base64.decode(stored, Base64.NO_WRAP), iv)
        }
        val passphrase = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val (cipherText, iv) = encrypt(passphrase)
        prefs.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(cipherText, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .commit()
        return passphrase
    }

    private fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return cipher.doFinal(data) to cipher.iv
    }

    private fun decrypt(cipherText: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "domina_db_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_CIPHERTEXT = "db_pass_ct"
        const val KEY_IV = "db_pass_iv"
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*DatabaseKeyProviderTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: Keystore-sealed SQLCipher passphrase provider"
```

---

## Task 3: Domain models (pure Kotlin)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/data/model/Mood.kt`, `FlowIntensity.kt`, `Energy.kt`, `CervicalMucus.kt`, `LhResult.kt`, `DayLog.kt`, `CycleEvent.kt`

- [ ] **Step 1: Write the failing unit test**

`app/src/test/java/com/domina/cycle/data/model/DayLogTest.kt`:
```kotlin
package com.domina.cycle.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DayLogTest {
    @Test fun emptyLogHasNoData() {
        val log = DayLog(date = LocalDate.of(2026, 5, 24))
        assertThat(log.isEmpty()).isTrue()
    }

    @Test fun logWithMoodIsNotEmpty() {
        val log = DayLog(date = LocalDate.of(2026, 5, 24), mood = Mood.HAPPY)
        assertThat(log.isEmpty()).isFalse()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*DayLogTest*"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement the enums and models**

`Mood.kt`:
```kotlin
package com.domina.cycle.data.model
enum class Mood { HAPPY, CALM, SENSITIVE, SAD, IRRITABLE, ANXIOUS, ENERGETIC, TIRED }
```
`FlowIntensity.kt`:
```kotlin
package com.domina.cycle.data.model
enum class FlowIntensity { NONE, SPOTTING, LIGHT, MEDIUM, HEAVY }
```
`Energy.kt`:
```kotlin
package com.domina.cycle.data.model
enum class Energy { LOW, MEDIUM, HIGH }
```
`CervicalMucus.kt`:
```kotlin
package com.domina.cycle.data.model
enum class CervicalMucus { DRY, STICKY, CREAMY, WATERY, EGG_WHITE }
```
`LhResult.kt`:
```kotlin
package com.domina.cycle.data.model
enum class LhResult { NOT_TESTED, NEGATIVE, POSITIVE }
```
`DayLog.kt`:
```kotlin
package com.domina.cycle.data.model

import java.time.LocalDate

data class DayLog(
    val date: LocalDate,
    val mood: Mood? = null,
    val energy: Energy? = null,
    val flow: FlowIntensity = FlowIntensity.NONE,
    val symptoms: List<String> = emptyList(),
    val bbt: Double? = null,            // basal body temperature, °C
    val cervicalMucus: CervicalMucus? = null,
    val lhResult: LhResult = LhResult.NOT_TESTED,
    val libido: Int? = null,            // 0..3
    val sleepHours: Double? = null,
    val weight: Double? = null,
    val note: String = "",
) {
    fun isEmpty(): Boolean =
        mood == null && energy == null && flow == FlowIntensity.NONE && symptoms.isEmpty() &&
            bbt == null && cervicalMucus == null && lhResult == LhResult.NOT_TESTED &&
            libido == null && sleepHours == null && weight == null && note.isBlank()
}
```
`CycleEvent.kt`:
```kotlin
package com.domina.cycle.data.model

import java.time.LocalDate

/** A period boundary, used to anchor cycle predictions in later phases. */
data class CycleEvent(
    val id: Long = 0,
    val date: LocalDate,
    val type: Type,
) {
    enum class Type { PERIOD_START, PERIOD_END }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*DayLogTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: pure-Kotlin domain models for day logs and cycle events"
```

---

## Task 4: Room entities, converters, DAOs

**Files:**
- Create: `app/src/main/java/com/domina/cycle/data/db/entity/DayLogEntity.kt`, `entity/CycleEventEntity.kt`, `Converters.kt`, `DayLogDao.kt`, `CycleEventDao.kt`

- [ ] **Step 1: Write the failing unit test for Converters (pure logic)**

`app/src/test/java/com/domina/cycle/data/db/ConvertersTest.kt`:
```kotlin
package com.domina.cycle.data.db

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {
    private val c = Converters()

    @Test fun symptomsRoundTrip() {
        val list = listOf("cramps", "headache")
        assertThat(c.toSymptomList(c.fromSymptomList(list))).isEqualTo(list)
    }

    @Test fun emptySymptomsRoundTrip() {
        assertThat(c.toSymptomList(c.fromSymptomList(emptyList()))).isEmpty()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ConvertersTest*"`
Expected: FAIL — `Converters` unresolved.

- [ ] **Step 3: Implement entities, converters, DAOs**

`Converters.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromSymptomList(list: List<String>): String = list.joinToString("")
    @TypeConverter fun toSymptomList(s: String): List<String> =
        if (s.isEmpty()) emptyList() else s.split("")
}
```
`entity/DayLogEntity.kt` (stores the date as an ISO string PK; enums as names):
```kotlin
package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_logs")
data class DayLogEntity(
    @PrimaryKey val date: String,          // ISO yyyy-MM-dd
    val mood: String? = null,
    val energy: String? = null,
    val flow: String = "NONE",
    val symptoms: List<String> = emptyList(),
    val bbt: Double? = null,
    val cervicalMucus: String? = null,
    val lhResult: String = "NOT_TESTED",
    val libido: Int? = null,
    val sleepHours: Double? = null,
    val weight: Double? = null,
    val note: String = "",
)
```
`entity/CycleEventEntity.kt`:
```kotlin
package com.domina.cycle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycle_events")
data class CycleEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,     // ISO yyyy-MM-dd
    val type: String,     // PERIOD_START / PERIOD_END
)
```
`DayLogDao.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.domina.cycle.data.db.entity.DayLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DayLogEntity)

    @Query("SELECT * FROM day_logs WHERE date = :date")
    suspend fun getByDate(date: String): DayLogEntity?

    @Query("SELECT * FROM day_logs WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeRange(start: String, end: String): Flow<List<DayLogEntity>>
}
```
`CycleEventDao.kt`:
```kotlin
package com.domina.cycle.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.domina.cycle.data.db.entity.CycleEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleEventDao {
    @Insert suspend fun insert(entity: CycleEventEntity): Long

    @Query("SELECT * FROM cycle_events ORDER BY date")
    fun observeAll(): Flow<List<CycleEventEntity>>
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*ConvertersTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: Room entities, type converters, and DAOs"
```

---

## Task 5: Encrypted AppDatabase + Hilt module + encryption proof

**Files:**
- Create: `app/src/main/java/com/domina/cycle/data/db/AppDatabase.kt`
- Create: `app/src/main/java/com/domina/cycle/di/DatabaseModule.kt`
- Test: `app/src/androidTest/java/com/domina/cycle/data/db/EncryptedDatabaseTest.kt`

- [ ] **Step 1: Write the failing instrumented test (round-trip + wrong-key rejection)**

```kotlin
package com.domina.cycle.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.entity.DayLogEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dbName = "enc-test.db"

    private fun open(pass: ByteArray): AppDatabase {
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(ctx, AppDatabase::class.java, dbName)
            .openHelperFactory(SupportOpenHelperFactory(pass))
            .build()
    }

    @Test fun writesAndReadsBackWithCorrectKey() = runBlocking {
        ctx.deleteDatabase(dbName)
        val key = "correct-horse".toByteArray()
        val db = open(key)
        db.dayLogDao().upsert(DayLogEntity(date = "2026-05-24", mood = "HAPPY"))
        db.close()

        val reopened = open(key)
        assertThat(reopened.dayLogDao().getByDate("2026-05-24")?.mood).isEqualTo("HAPPY")
        reopened.close()
    }

    @Test(expected = Exception::class)
    fun wrongKeyCannotOpen() = runBlocking {
        ctx.deleteDatabase(dbName)
        open("correct-horse".toByteArray()).apply {
            dayLogDao().upsert(DayLogEntity(date = "2026-05-24"))
            close()
        }
        val bad = open("wrong-key".toByteArray())
        bad.dayLogDao().getByDate("2026-05-24") // must throw: file is encrypted
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*EncryptedDatabaseTest*"`
Expected: FAIL — `AppDatabase` unresolved.

- [ ] **Step 3: Implement `AppDatabase`**

```kotlin
package com.domina.cycle.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.domina.cycle.data.db.entity.CycleEventEntity
import com.domina.cycle.data.db.entity.DayLogEntity

@Database(
    entities = [DayLogEntity::class, CycleEventEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun cycleEventDao(): CycleEventDao
}
```

- [ ] **Step 4: Implement `DatabaseModule` (wires SQLCipher with the Keystore passphrase)**

```kotlin
package com.domina.cycle.di

import android.content.Context
import androidx.room.Room
import com.domina.cycle.data.db.AppDatabase
import com.domina.cycle.data.db.CycleEventDao
import com.domina.cycle.data.db.DayLogDao
import com.domina.cycle.security.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = DatabaseKeyProvider(context).getOrCreatePassphrase()
        return Room.databaseBuilder(context, AppDatabase::class.java, "domina.db")
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }

    @Provides fun provideDayLogDao(db: AppDatabase): DayLogDao = db.dayLogDao()
    @Provides fun provideCycleEventDao(db: AppDatabase): CycleEventDao = db.cycleEventDao()
}
```

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*EncryptedDatabaseTest*"`
Expected: PASS (both tests). The `wrongKeyCannotOpen` test proves the file is genuinely encrypted.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: SQLCipher-encrypted Room database wired via Hilt"
```

---

## Task 6: Repositories (interfaces + Room impls)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/data/repository/DayLogRepository.kt`, `CycleRepository.kt`, `RoomDayLogRepository.kt`, `RoomCycleRepository.kt`
- Create: `app/src/main/java/com/domina/cycle/di/RepositoryModule.kt`
- Test: `app/src/androidTest/java/com/domina/cycle/data/repository/RoomDayLogRepositoryTest.kt`

- [ ] **Step 1: Write the failing instrumented test (in-memory Room)**

```kotlin
package com.domina.cycle.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.AppDatabase
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.Mood
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RoomDayLogRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: DayLogRepository

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = RoomDayLogRepository(db.dayLogDao())
    }
    @After fun tearDown() = db.close()

    @Test fun savesAndLoadsDayLogWithEnumMapping() = runBlocking {
        val date = LocalDate.of(2026, 5, 24)
        repo.save(DayLog(date = date, mood = Mood.HAPPY, symptoms = listOf("cramps")))
        val loaded = repo.getByDate(date)
        assertThat(loaded?.mood).isEqualTo(Mood.HAPPY)
        assertThat(loaded?.symptoms).containsExactly("cramps")
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*RoomDayLogRepositoryTest*"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement interfaces and Room mapping impls**

`DayLogRepository.kt`:
```kotlin
package com.domina.cycle.data.repository

import com.domina.cycle.data.model.DayLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DayLogRepository {
    suspend fun save(log: DayLog)
    suspend fun getByDate(date: LocalDate): DayLog?
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayLog>>
}
```
`CycleRepository.kt`:
```kotlin
package com.domina.cycle.data.repository

import com.domina.cycle.data.model.CycleEvent
import kotlinx.coroutines.flow.Flow

interface CycleRepository {
    suspend fun add(event: CycleEvent)
    fun observeAll(): Flow<List<CycleEvent>>
}
```
`RoomDayLogRepository.kt`:
```kotlin
package com.domina.cycle.data.repository

import com.domina.cycle.data.db.DayLogDao
import com.domina.cycle.data.db.entity.DayLogEntity
import com.domina.cycle.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RoomDayLogRepository @Inject constructor(
    private val dao: DayLogDao,
) : DayLogRepository {

    override suspend fun save(log: DayLog) = dao.upsert(log.toEntity())

    override suspend fun getByDate(date: LocalDate): DayLog? =
        dao.getByDate(date.toString())?.toModel()

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayLog>> =
        dao.observeRange(start.toString(), end.toString()).map { list -> list.map { it.toModel() } }
}

private fun DayLog.toEntity() = DayLogEntity(
    date = date.toString(),
    mood = mood?.name,
    energy = energy?.name,
    flow = flow.name,
    symptoms = symptoms,
    bbt = bbt,
    cervicalMucus = cervicalMucus?.name,
    lhResult = lhResult.name,
    libido = libido,
    sleepHours = sleepHours,
    weight = weight,
    note = note,
)

private fun DayLogEntity.toModel() = DayLog(
    date = LocalDate.parse(date),
    mood = mood?.let { Mood.valueOf(it) },
    energy = energy?.let { Energy.valueOf(it) },
    flow = FlowIntensity.valueOf(flow),
    symptoms = symptoms,
    bbt = bbt,
    cervicalMucus = cervicalMucus?.let { CervicalMucus.valueOf(it) },
    lhResult = LhResult.valueOf(lhResult),
    libido = libido,
    sleepHours = sleepHours,
    weight = weight,
    note = note,
)
```
`RoomCycleRepository.kt`:
```kotlin
package com.domina.cycle.data.repository

import com.domina.cycle.data.db.CycleEventDao
import com.domina.cycle.data.db.entity.CycleEventEntity
import com.domina.cycle.data.model.CycleEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RoomCycleRepository @Inject constructor(
    private val dao: CycleEventDao,
) : CycleRepository {
    override suspend fun add(event: CycleEvent) {
        dao.insert(CycleEventEntity(date = event.date.toString(), type = event.type.name))
    }
    override fun observeAll(): Flow<List<CycleEvent>> =
        dao.observeAll().map { list ->
            list.map { CycleEvent(it.id, LocalDate.parse(it.date), CycleEvent.Type.valueOf(it.type)) }
        }
}
```

- [ ] **Step 4: Implement `RepositoryModule`**

```kotlin
package com.domina.cycle.di

import com.domina.cycle.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindDayLogRepository(impl: RoomDayLogRepository): DayLogRepository

    @Binds @Singleton
    abstract fun bindCycleRepository(impl: RoomCycleRepository): CycleRepository
}
```

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*RoomDayLogRepositoryTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: day-log and cycle repositories with model<->entity mapping"
```

---

## Task 7: Theming (3 switchable themes) + settings persistence

**Files:**
- Create: `app/src/main/java/com/domina/cycle/data/prefs/ThemePreference.kt`, `SettingsRepository.kt`
- Create: `app/src/main/java/com/domina/cycle/ui/theme/Color.kt`, `Theme.kt`, `Type.kt`
- Create: `app/src/main/java/com/domina/cycle/di/PrefsModule.kt`
- Test: `app/src/test/java/com/domina/cycle/data/prefs/ThemePreferenceTest.kt`

- [ ] **Step 1: Write the failing unit test for the pure string mapping**

```kotlin
package com.domina.cycle.data.prefs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemePreferenceTest {
    @Test fun mapsEachThemeToStableKeyAndBack() {
        ThemePreference.entries.forEach { theme ->
            assertThat(ThemePreference.fromKey(theme.key)).isEqualTo(theme)
        }
    }
    @Test fun unknownKeyFallsBackToSoftSweet() {
        assertThat(ThemePreference.fromKey("garbage")).isEqualTo(ThemePreference.SOFT_SWEET)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ThemePreferenceTest*"`
Expected: FAIL — `ThemePreference` unresolved.

- [ ] **Step 3: Implement `ThemePreference`**

```kotlin
package com.domina.cycle.data.prefs

enum class ThemePreference(val key: String, val displayName: String) {
    SOFT_SWEET("soft_sweet", "Soft & Sweet"),
    BRIGHT_JOYFUL("bright_joyful", "Bright & Joyful"),
    WARM_COZY("warm_cozy", "Warm & Cozy");

    companion object {
        fun fromKey(key: String?): ThemePreference =
            entries.firstOrNull { it.key == key } ?: SOFT_SWEET
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*ThemePreferenceTest*"`
Expected: PASS.

- [ ] **Step 5: Implement `SettingsRepository` (DataStore) and Hilt module**

`SettingsRepository.kt`:
```kotlin
package com.domina.cycle.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeKey = stringPreferencesKey("theme")

    val theme: Flow<ThemePreference> =
        context.dataStore.data.map { ThemePreference.fromKey(it[themeKey]) }

    suspend fun setTheme(theme: ThemePreference) {
        context.dataStore.edit { it[themeKey] = theme.key }
    }
}
```
`PrefsModule.kt` — not needed: `SettingsRepository` is `@Inject` constructable. Skip a module.

- [ ] **Step 6: Implement theme colors and `AppTheme`**

`Type.kt`:
```kotlin
package com.domina.cycle.ui.theme

import androidx.compose.material3.Typography
val AppTypography = Typography()
```
`Color.kt`:
```kotlin
package com.domina.cycle.ui.theme

import androidx.compose.ui.graphics.Color

// Soft & Sweet
val SoftPrimary = Color(0xFF7C5CBF)
val SoftSecondary = Color(0xFFC76B9A)
val SoftBackground = Color(0xFFFDF4FA)
val SoftSurface = Color(0xFFFFFFFF)

// Bright & Joyful
val BrightPrimary = Color(0xFF1FB6A6)
val BrightSecondary = Color(0xFFFF5470)
val BrightBackground = Color(0xFFFFF8EE)
val BrightSurface = Color(0xFFFFFFFF)

// Warm & Cozy
val CozyPrimary = Color(0xFF5E7A4F)
val CozySecondary = Color(0xFFA87C4F)
val CozyBackground = Color(0xFFF7F1E7)
val CozySurface = Color(0xFFFFFDF9)
```
`Theme.kt`:
```kotlin
package com.domina.cycle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.domina.cycle.data.prefs.ThemePreference

private fun schemeFor(theme: ThemePreference) = when (theme) {
    ThemePreference.SOFT_SWEET -> lightColorScheme(
        primary = SoftPrimary, secondary = SoftSecondary,
        background = SoftBackground, surface = SoftSurface,
    )
    ThemePreference.BRIGHT_JOYFUL -> lightColorScheme(
        primary = BrightPrimary, secondary = BrightSecondary,
        background = BrightBackground, surface = BrightSurface,
    )
    ThemePreference.WARM_COZY -> lightColorScheme(
        primary = CozyPrimary, secondary = CozySecondary,
        background = CozyBackground, surface = CozySurface,
    )
}

@Composable
fun AppTheme(theme: ThemePreference, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = schemeFor(theme), typography = AppTypography, content = content)
}
```

- [ ] **Step 7: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: three switchable Compose themes persisted via DataStore"
```

---

## Task 8: App lock (PIN + biometric)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/security/PinManager.kt`, `BiometricAuthenticator.kt`
- Create: `app/src/main/java/com/domina/cycle/ui/lock/LockViewModel.kt`, `LockScreen.kt`
- Test: `app/src/test/java/com/domina/cycle/security/PinManagerTest.kt`

- [ ] **Step 1: Write the failing unit test for PIN hashing**

```kotlin
package com.domina.cycle.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PinManagerTest {
    @Test fun correctPinVerifies() {
        val stored = PinManager.hash("1234")
        assertThat(PinManager.verify("1234", stored)).isTrue()
    }
    @Test fun wrongPinFails() {
        val stored = PinManager.hash("1234")
        assertThat(PinManager.verify("0000", stored)).isFalse()
    }
    @Test fun sameInputDifferentSaltDifferentHash() {
        assertThat(PinManager.hash("1234")).isNotEqualTo(PinManager.hash("1234"))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PinManagerTest*"`
Expected: FAIL — `PinManager` unresolved.

- [ ] **Step 3: Implement `PinManager` (PBKDF2, salted, pure JVM)**

```kotlin
package com.domina.cycle.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 PIN hashing. Stored form is "salt:hash" in Base64. */
object PinManager {
    private const val ITERATIONS = 120_000
    private const val KEY_LEN = 256

    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt)
        return "${b64(salt)}:${b64(hash)}"
    }

    fun verify(pin: String, stored: String): Boolean {
        val (saltB64, hashB64) = stored.split(":").let { it[0] to it[1] }
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        return constantEquals(pbkdf2(pin, salt), Base64.decode(hashB64, Base64.NO_WRAP))
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LEN)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun constantEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var r = 0; for (i in a.indices) r = r or (a[i].toInt() xor b[i].toInt()); return r == 0
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
}
```

> Note: `android.util.Base64` is available in unit tests via the bundled stubs only if `testOptions.unitTests.isReturnDefaultValues` is false and the class is exercised on-device. If the unit test fails to find `Base64`, move `PinManagerTest` to `androidTest/` (instrumented) — the implementation is unchanged. Verify in Step 4.

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PinManagerTest*"`
Expected: PASS. If `Base64` is unresolved at runtime, move the test file to `app/src/androidTest/...` and run `:app:connectedDebugAndroidTest --tests "*PinManagerTest*"` — expect PASS.

- [ ] **Step 5: Implement `BiometricAuthenticator` (manual verification)**

```kotlin
package com.domina.cycle.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class BiometricAuthenticator(private val activity: FragmentActivity) {

    fun isAvailable(): Boolean =
        BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS

    suspend fun authenticate(): Boolean = suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    if (cont.isActive) cont.resume(false)
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock")
                .setSubtitle("Confirm it's you")
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        )
    }
}
```

> `MainActivity` must extend `FragmentActivity` (it does — `ComponentActivity` is a `FragmentActivity` subclass via `androidx.activity.ComponentActivity`). Confirm imports compile.

- [ ] **Step 6: Implement `LockViewModel` and `LockScreen`**

`LockViewModel.kt`:
```kotlin
package com.domina.cycle.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun onPinEntered(pin: String) {
        viewModelScope.launch {
            val stored = settings.pinHash.first()
            if (stored == null) { settings.setPinHash(PinManager.hash(pin)); _unlocked.value = true }
            else if (PinManager.verify(pin, stored)) _unlocked.value = true
        }
    }
    fun onBiometricSuccess() { _unlocked.value = true }
}
```
Add to `SettingsRepository` (extends Task 7 file):
```kotlin
// add inside SettingsRepository:
private val pinKey = stringPreferencesKey("pin_hash")
val pinHash: Flow<String?> = context.dataStore.data.map { it[pinKey] }
suspend fun setPinHash(hash: String) { context.dataStore.edit { it[pinKey] = hash } }
```
`LockScreen.kt`:
```kotlin
package com.domina.cycle.ui.lock

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    hasPin: Boolean,
    onPinEntered: (String) -> Unit,
    onUseBiometric: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (hasPin) "Welcome back 💛" else "Set a PIN to begin 💛", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) }, label = { Text("PIN") })
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onPinEntered(pin); pin = "" }, enabled = pin.length >= 4) {
            Text(if (hasPin) "Unlock" else "Save PIN")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onUseBiometric) { Text("Use fingerprint / face") }
    }
}
```

- [ ] **Step 7: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: PIN + biometric app lock"
```

---

## Task 9: Navigation, MainActivity lock gate, Today/Log/Calendar screens

**Files:**
- Create: `app/src/main/java/com/domina/cycle/ui/nav/Destinations.kt`, `AppNav.kt`
- Create: `app/src/main/java/com/domina/cycle/ui/today/TodayViewModel.kt`, `TodayScreen.kt`
- Create: `app/src/main/java/com/domina/cycle/ui/log/LogViewModel.kt`, `LogScreen.kt`
- Create: `app/src/main/java/com/domina/cycle/ui/calendar/CalendarViewModel.kt`, `CalendarScreen.kt`
- Modify: `app/src/main/java/com/domina/cycle/MainActivity.kt`
- Test: `app/src/test/java/com/domina/cycle/ui/log/LogViewModelTest.kt`

- [ ] **Step 1: Write the failing unit test for `LogViewModel` (with a fake repository)**

```kotlin
package com.domina.cycle.ui.log

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.Mood
import com.domina.cycle.data.repository.DayLogRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FakeDayLogRepository : DayLogRepository {
    val saved = mutableMapOf<LocalDate, DayLog>()
    override suspend fun save(log: DayLog) { saved[log.date] = log }
    override suspend fun getByDate(date: LocalDate) = saved[date]
    override fun observeRange(start: LocalDate, end: LocalDate) =
        flowOf(saved.values.filter { it.date in start..end })
}

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun savingPersistsTheEditedLog() = runTest(dispatcher) {
        val repo = FakeDayLogRepository()
        val vm = LogViewModel(repo)
        val date = LocalDate.of(2026, 5, 24)
        vm.load(date)
        advanceUntilIdle()
        vm.setMood(Mood.HAPPY)
        vm.save()
        advanceUntilIdle()
        assertThat(repo.saved[date]?.mood).isEqualTo(Mood.HAPPY)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*LogViewModelTest*"`
Expected: FAIL — `LogViewModel` unresolved.

- [ ] **Step 3: Implement `LogViewModel`**

```kotlin
package com.domina.cycle.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.*
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val repository: DayLogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DayLog(date = LocalDate.now()))
    val state: StateFlow<DayLog> = _state.asStateFlow()

    fun load(date: LocalDate) {
        viewModelScope.launch {
            _state.value = repository.getByDate(date) ?: DayLog(date = date)
        }
    }
    fun setMood(mood: Mood) { _state.update { it.copy(mood = mood) } }
    fun setFlow(flow: FlowIntensity) { _state.update { it.copy(flow = flow) } }
    fun setNote(note: String) { _state.update { it.copy(note = note) } }
    fun toggleSymptom(s: String) = _state.update {
        it.copy(symptoms = if (s in it.symptoms) it.symptoms - s else it.symptoms + s)
    }
    fun save() { viewModelScope.launch { repository.save(_state.value) } }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*LogViewModelTest*"`
Expected: PASS.

- [ ] **Step 5: Implement `TodayViewModel` and `CalendarViewModel`**

`TodayViewModel.kt`:
```kotlin
package com.domina.cycle.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    repository: DayLogRepository,
) : ViewModel() {
    val today: LocalDate = LocalDate.now()
    val log: StateFlow<DayLog?> = flow { emit(repository.getByDate(today)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
```
`CalendarViewModel.kt`:
```kotlin
package com.domina.cycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: DayLogRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = month.asStateFlow()

    val logsThisMonth: StateFlow<List<DayLog>> = month.flatMapLatest { m ->
        repository.observeRange(m.atDay(1), m.atEndOfMonth())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun prevMonth() { month.value = month.value.minusMonths(1) }
}
```

- [ ] **Step 6: Implement screens and navigation**

`Destinations.kt`:
```kotlin
package com.domina.cycle.ui.nav

object Destinations {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val LOG = "log"
}
```
`TodayScreen.kt`:
```kotlin
package com.domina.cycle.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TodayScreen(onLogToday: () -> Unit, vm: TodayViewModel = hiltViewModel()) {
    val log by vm.log.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hello 💛", style = MaterialTheme.typography.headlineMedium)
        Text("Today: ${vm.today}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Today's log", style = MaterialTheme.typography.titleMedium)
                Text(if (log == null || log!!.isEmpty()) "Nothing logged yet" else "Mood: ${log!!.mood ?: "—"} · Flow: ${log!!.flow}")
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onLogToday) { Text("Log today") }
    }
}
```
`LogScreen.kt`:
```kotlin
package com.domina.cycle.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.model.Mood
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(date: LocalDate, onSaved: () -> Unit, vm: LogViewModel = hiltViewModel()) {
    LaunchedEffect(date) { vm.load(date) }
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Log for $date", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("Mood")
        Row {
            Mood.entries.take(4).forEach { m ->
                FilterChip(selected = state.mood == m, onClick = { vm.setMood(m) }, label = { Text(m.name.lowercase()) }, modifier = Modifier.padding(end = 4.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Flow")
        Row {
            FlowIntensity.entries.forEach { f ->
                FilterChip(selected = state.flow == f, onClick = { vm.setFlow(f) }, label = { Text(f.name.lowercase()) }, modifier = Modifier.padding(end = 4.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = state.note, onValueChange = vm::setNote, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.save(); onSaved() }) { Text("Save") }
    }
}
```
`CalendarScreen.kt`:
```kotlin
package com.domina.cycle.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CalendarScreen(vm: CalendarViewModel = hiltViewModel()) {
    val month by vm.visibleMonth.collectAsState()
    val logs by vm.logsThisMonth.collectAsState()
    val loggedDays = logs.map { it.date.dayOfMonth }.toSet()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = vm::prevMonth) { Text("‹") }
            Text("$month", Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = vm::nextMonth) { Text("›") }
        }
        val days = (1..month.lengthOfMonth()).toList()
        LazyVerticalGrid(columns = GridCells.Fixed(7)) {
            items(days) { d ->
                Box(Modifier.padding(4.dp)) {
                    Surface(
                        color = if (d in loggedDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ) { Text("$d", Modifier.padding(10.dp)) }
                }
            }
        }
    }
}
```
`AppNav.kt`:
```kotlin
package com.domina.cycle.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.domina.cycle.ui.calendar.CalendarScreen
import com.domina.cycle.ui.log.LogScreen
import com.domina.cycle.ui.today.TodayScreen
import java.time.LocalDate

@Composable
fun AppNav() {
    val nav = rememberNavController()
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = true, onClick = { nav.navigate(Destinations.TODAY) },
                icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Today") })
            NavigationBarItem(selected = false, onClick = { nav.navigate(Destinations.CALENDAR) },
                icon = { Icon(Icons.Filled.CalendarMonth, null) }, label = { Text("Calendar") })
        }
    }) { padding ->
        NavHost(nav, startDestination = Destinations.TODAY, modifier = Modifier.padding(padding)) {
            composable(Destinations.TODAY) { TodayScreen(onLogToday = { nav.navigate(Destinations.LOG) }) }
            composable(Destinations.CALENDAR) { CalendarScreen() }
            composable(Destinations.LOG) { LogScreen(date = LocalDate.now(), onSaved = { nav.popBackStack() }) }
        }
    }
}
```

- [ ] **Step 7: Rewrite `MainActivity` to gate the app behind the lock + apply theme**

```kotlin
package com.domina.cycle

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.security.BiometricAuthenticator
import com.domina.cycle.ui.lock.LockScreen
import com.domina.cycle.ui.lock.LockViewModel
import com.domina.cycle.ui.nav.AppNav
import com.domina.cycle.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.activity.viewModels

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settings: SettingsRepository
    private val lockVm: LockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bio = BiometricAuthenticator(this)
        setContent {
            val theme by settings.theme.collectAsStateWithLifecycle(
                initialValue = com.domina.cycle.data.prefs.ThemePreference.SOFT_SWEET
            )
            val unlocked by lockVm.unlocked.collectAsStateWithLifecycle()
            var hasPin by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) { hasPin = settings.pinHash.first() != null }

            AppTheme(theme) {
                if (unlocked) {
                    AppNav()
                } else {
                    LockScreen(
                        hasPin = hasPin == true,
                        onPinEntered = lockVm::onPinEntered,
                        onUseBiometric = {
                            if (bio.isAvailable()) lifecycleScope.launch {
                                if (bio.authenticate()) lockVm.onBiometricSuccess()
                            }
                        },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 8: Build, install, manually verify the full flow**

Run:
```bash
./gradlew :app:installDebug
```
Manual checks on the device:
1. First launch → "Set a PIN" → enter 4+ digits → Save → app unlocks to Today.
2. Kill & relaunch → "Welcome back" → wrong PIN stays locked, correct PIN unlocks.
3. "Use fingerprint / face" → biometric prompt unlocks (if enrolled).
4. Today → "Log today" → set mood/flow/note → Save → returns to Today showing the values.
5. Calendar tab → today's cell is highlighted; prev/next month works.

- [ ] **Step 9: Run the full test suites**

Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: navigation, lock gate, Today/Log/Calendar screens (Phase 1 MVP)"
```

---

## Phase 1 Definition of Done

- App builds (`assembleDebug`) and installs.
- All unit tests (`testDebugUnitTest`) and instrumented tests (`connectedDebugAndroidTest`) pass.
- Database file is provably encrypted (wrong-key test fails to open).
- App opens behind a PIN/biometric lock.
- User can pick any of 3 themes (wired; a Settings screen to switch lands in a later phase — for now `SOFT_SWEET` default is applied and the system is in place).
- User can log a day (mood/flow/note) and see it on Today and Calendar.
- No `INTERNET` permission anywhere in the merged manifest (verify: `./gradlew :app:assembleDebug` then grep the merged manifest at `app/build/intermediates/merged_manifests/debug/AndroidManifest.xml` for `INTERNET` — expect no match).

---

## Self-Review Notes (author)

- **Spec coverage (Phase 1 scope):** scaffolding ✓, encryption (Keystore+SQLCipher) ✓, data model ✓, repositories ✓, 3 themes ✓, biometric+PIN lock ✓, daily logging ✓, calendar ✓, Today dashboard ✓. Predictions, notifications, pregnancy, insights, extras are explicitly later phases (see roadmap).
- **Deferred to later phases (intentionally):** a Settings screen to switch theme/manage PIN/mode (Phase 2+), clinical fields beyond mood/flow/note in the Log UI (the data model already supports BBT/mucus/LH/etc.; the UI exposes them in Phase 2 alongside TTC mode).
- **Known verification caveat:** `PinManagerTest` uses `android.util.Base64`; if the JVM unit test cannot resolve it, the plan instructs moving that one test to `androidTest`. Implementation code is unchanged either way.
