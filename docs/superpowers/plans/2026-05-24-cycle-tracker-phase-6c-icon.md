# Phase 6c: Custom & Discreet App Icon — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give Domina a warm custom launcher icon, and an optional **discreet disguise** — a Settings toggle that makes the app appear on the home screen as a generic "Notes" icon/name, so it doesn't advertise what it is.

**Architecture:** A custom **adaptive icon** (vector foreground + background; minSdk 26 supports adaptive icons everywhere). The discreet mode uses **two `activity-alias` launcher entries** both targeting `MainActivity` (the real one, and a "Notes"-labelled one); a `DiscreetIconManager` flips which alias is enabled via `PackageManager.setComponentEnabledSetting`, and the choice persists in DataStore. Exactly one launcher alias is enabled at all times, so the app never disappears.

**Tech Stack:** Android adaptive icons (vector drawables), PackageManager component toggling, DataStore, Compose. No new dependencies. Still **no `INTERNET` permission**.

> No unit tests here (resource + manifest + system-component work). Verify by build + install + confirming the launcher resolves and the app launches; the discreet swap is verified by checking the resolved launcher label via `adb`.

## Builds on (existing, on `main`)
- `AndroidManifest.xml` — `MainActivity` currently has the `MAIN`/`LAUNCHER` intent-filter and `android:label="@string/app_name"`.
- `data/prefs/SettingsRepository.kt` (DataStore), `ui/settings/{SettingsScreen,SettingsViewModel}.kt`.

## New / changed files
```
app/src/main/res/drawable/ic_launcher_foreground.xml   # crescent-moon mark
app/src/main/res/drawable/ic_launcher_background.xml    # soft solid background
app/src/main/res/drawable/ic_discreet_foreground.xml    # generic "note" mark
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml      # adaptive icon (+ _round)
app/src/main/res/mipmap-anydpi-v26/ic_discreet.xml      # discreet adaptive icon
app/src/main/res/values/colors.xml                      # icon background colors
app/src/main/AndroidManifest.xml                        # icon refs + 2 activity-aliases
app/src/main/java/com/domina/cycle/launcher/DiscreetIconManager.kt
app/src/main/java/com/domina/cycle/data/prefs/SettingsRepository.kt   # discreet pref
app/src/main/java/com/domina/cycle/ui/settings/{SettingsViewModel,SettingsScreen}.kt
```

---

## Task 1: Custom adaptive launcher icon

**Files:** the drawables, mipmaps, colors.xml, and manifest icon refs above.

- [ ] **Step 1: Add icon colors**

Create/extend `app/src/main/res/values/colors.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_bg">#FF7C5CBF</color>
    <color name="ic_discreet_bg">#FF5B6470</color>
</resources>
```

- [ ] **Step 2: Add the foreground + background drawables**

`app/src/main/res/drawable/ic_launcher_background.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="@color/ic_launcher_bg" android:pathData="M0,0h108v108h-108z"/>
</vector>
```
`app/src/main/res/drawable/ic_launcher_foreground.xml` (a crescent moon + star, centered in the adaptive safe zone):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFDF4FA"
        android:pathData="M66,30a24,24 0,1 0,0 48a24,24 0,0 1,0 -48z"/>
    <path android:fillColor="#FFFFE9A8"
        android:pathData="M44,40l2.2,5.6 6,0.4 -4.6,3.8 1.5,5.8 -5.1,-3.2 -5.1,3.2 1.5,-5.8 -4.6,-3.8 6,-0.4z"/>
</vector>
```
`app/src/main/res/drawable/ic_discreet_foreground.xml` (a plain note/lines mark):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFFFF" android:pathData="M36,32h36a4,4 0,0 1,4 4v40a4,4 0,0 1,-4 4h-36a4,4 0,0 1,-4 -4v-40a4,4 0,0 1,4 -4z"/>
    <path android:fillColor="#FF5B6470" android:pathData="M40,44h28v4h-28z M40,54h28v4h-28z M40,64h18v4h-18z"/>
</vector>
```

- [ ] **Step 3: Add the adaptive icon mipmaps**

`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```
`app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — identical content to `ic_launcher.xml`.
`app/src/main/res/mipmap-anydpi-v26/ic_discreet.xml`:
```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_discreet_background"/>
    <foreground android:drawable="@drawable/ic_discreet_foreground"/>
</adaptive-icon>
```
Also add `app/src/main/res/drawable/ic_discreet_background.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="@color/ic_discreet_bg" android:pathData="M0,0h108v108h-108z"/>
</vector>
```

- [ ] **Step 4: Reference the icon in the manifest**

In `AndroidManifest.xml` `<application>`, add `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`.

- [ ] **Step 5: Build, install, verify the icon resource & launch**

Run: `./gradlew :app:installDebug` → BUILD SUCCESSFUL. Launch (`adb shell am start -n com.domina.cycle/.MainActivity`); `adb logcat -d` shows no crash. The launcher icon is now the custom moon (visible in the app drawer).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/ app/src/main/AndroidManifest.xml
git commit -m "feat: custom adaptive launcher icon"
```

---

## Task 2: Discreet-mode aliases + toggle

**Files:** manifest aliases, `DiscreetIconManager`, SettingsRepository pref, Settings UI.

- [ ] **Step 1: Refactor the manifest to use two launcher aliases**

In `AndroidManifest.xml`: **remove the `<intent-filter>` (MAIN/LAUNCHER) from `MainActivity`** and set `MainActivity` `android:exported="false"` (it will be launched via an alias). Then add two aliases inside `<application>` (after the activity):
```xml
        <activity-alias
            android:name=".LauncherDefault"
            android:targetActivity=".MainActivity"
            android:exported="true"
            android:enabled="true"
            android:icon="@mipmap/ic_launcher"
            android:roundIcon="@mipmap/ic_launcher_round"
            android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>

        <activity-alias
            android:name=".LauncherDiscreet"
            android:targetActivity=".MainActivity"
            android:exported="true"
            android:enabled="false"
            android:icon="@mipmap/ic_discreet"
            android:label="Notes">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>
```
(Keep `MainActivity` declared, just without the launcher intent-filter.)

- [ ] **Step 2: Implement `DiscreetIconManager`**

```kotlin
package com.domina.cycle.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Swaps between the real launcher alias and a discreet "Notes" alias. */
class DiscreetIconManager(private val context: Context) {
    private val default = ComponentName(context, "com.domina.cycle.LauncherDefault")
    private val discreet = ComponentName(context, "com.domina.cycle.LauncherDiscreet")

    fun setDiscreet(enabled: Boolean) {
        val pm = context.packageManager
        // Exactly one launcher alias is enabled at any time.
        pm.setComponentEnabledSetting(
            discreet,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        pm.setComponentEnabledSetting(
            default,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
```

- [ ] **Step 3: Persist the discreet preference**

Append to `SettingsRepository`:
```kotlin
    private val discreetKey = booleanPreferencesKey("discreet_icon")
    val discreetIcon: Flow<Boolean> = context.dataStore.data.map { it[discreetKey] ?: false }
    suspend fun setDiscreetIcon(enabled: Boolean) { context.dataStore.edit { it[discreetKey] = enabled } }
```
(`booleanPreferencesKey` is already imported from earlier phases.)

- [ ] **Step 4: Wire the Settings toggle**

In `SettingsViewModel` (inject `@ApplicationContext context` already present):
```kotlin
    val discreetIcon: StateFlow<Boolean> =
        settings.discreetIcon.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setDiscreetIcon(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDiscreetIcon(enabled)
            com.domina.cycle.launcher.DiscreetIconManager(context).setDiscreet(enabled)
        }
    }
```
In `SettingsScreen`, under a "Privacy" section, add a toggle row:
```kotlin
    val discreet by vm.discreetIcon.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Discreet icon")
            Text("Show on the home screen as \"Notes\"", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = discreet, onCheckedChange = { vm.setDiscreetIcon(it) })
    }
```

- [ ] **Step 5: Build, install, verify launcher resolution & the swap**

Run: `./gradlew :app:installDebug` → BUILD SUCCESSFUL. Then verify the default launcher resolves and the app launches:
```bash
adb shell cmd package resolve-activity -c android.intent.category.LAUNCHER com.domina.cycle | grep -iE "LauncherDefault|label"
adb shell monkey -p com.domina.cycle -c android.intent.category.LAUNCHER 1   # should launch, no error
```
Expected: the default alias resolves (label "Domina"); app launches without crash. (The real visual swap to "Notes" is the user's to toggle in Settings → Privacy.)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/AndroidManifest.xml \
        app/src/main/java/com/domina/cycle/launcher/DiscreetIconManager.kt \
        app/src/main/java/com/domina/cycle/data/prefs/SettingsRepository.kt \
        app/src/main/java/com/domina/cycle/ui/settings/
git commit -m "feat: discreet 'Notes' launcher alias with Settings toggle"
```

---

## Phase 6c Definition of Done

- The app has a custom adaptive launcher icon (warm moon mark).
- A Settings → Privacy "Discreet icon" toggle disguises the home-screen entry as "Notes" (and back), persisted across launches; exactly one launcher entry is always enabled (the app never disappears).
- App builds, installs, and the default launcher resolves & launches without crash. Still **no `INTERNET` permission**.

## Deferred
- A polished, designer-quality icon (this is a clean placeholder mark); additional disguise options.
```
