package com.domina.cycle.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.domina.cycle.domain.reminders.ReminderSettings
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
    private val pinKey = stringPreferencesKey("pin_hash")

    val theme: Flow<ThemePreference> =
        context.dataStore.data.map { ThemePreference.fromKey(it[themeKey]) }

    suspend fun setTheme(theme: ThemePreference) {
        context.dataStore.edit { it[themeKey] = theme.key }
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { it[pinKey] }

    suspend fun setPinHash(hash: String) {
        context.dataStore.edit { it[pinKey] = hash }
    }

    // --- reminder settings ---
    private val periodAlertsKey = booleanPreferencesKey("rem_period")
    private val fertilityAlertsKey = booleanPreferencesKey("rem_fertility")
    private val dailyNudgeKey = booleanPreferencesKey("rem_nudge")
    private val nudgeMinsKey = intPreferencesKey("rem_nudge_mins")

    val reminderSettings: Flow<ReminderSettings> =
        context.dataStore.data.map { p ->
            ReminderSettings(
                periodAlerts = p[periodAlertsKey] ?: true,
                fertilityAlerts = p[fertilityAlertsKey] ?: true,
                dailyNudge = p[dailyNudgeKey] ?: false,
                dailyNudgeTime = ReminderSettingsCodec.minutesToTime(p[nudgeMinsKey] ?: (20 * 60)),
            )
        }

    suspend fun setReminderSettings(s: ReminderSettings) {
        context.dataStore.edit {
            it[periodAlertsKey] = s.periodAlerts
            it[fertilityAlertsKey] = s.fertilityAlerts
            it[dailyNudgeKey] = s.dailyNudge
            it[nudgeMinsKey] = ReminderSettingsCodec.timeToMinutes(s.dailyNudgeTime)
        }
    }

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

    // Last-period date for which the "switch to pregnancy mode?" suggestion was dismissed.
    private val pregSuggestDismissKey =
        androidx.datastore.preferences.core.longPreferencesKey("preg_suggest_dismissed_lmp")
    val pregnancySuggestDismissedLmp: Flow<Long?> =
        context.dataStore.data.map { it[pregSuggestDismissKey] }
    suspend fun setPregnancySuggestDismissedLmp(epochDay: Long) {
        context.dataStore.edit { it[pregSuggestDismissKey] = epochDay }
    }

    // --- discreet icon ---
    private val discreetKey = booleanPreferencesKey("discreet_icon")
    val discreetIcon: Flow<Boolean> = context.dataStore.data.map { it[discreetKey] ?: false }
    suspend fun setDiscreetIcon(enabled: Boolean) { context.dataStore.edit { it[discreetKey] = enabled } }

    // --- user profile + onboarding ---
    private val nameKey = stringPreferencesKey("profile_name")
    private val dobKey = androidx.datastore.preferences.core.longPreferencesKey("profile_dob_epoch_day")
    private val heightKey = intPreferencesKey("profile_height_cm")
    private val onboardingKey = booleanPreferencesKey("onboarding_complete")

    val userName: Flow<String?> = context.dataStore.data.map { it[nameKey]?.ifBlank { null } }
    val birthDate: Flow<java.time.LocalDate?> =
        context.dataStore.data.map { p -> p[dobKey]?.let { java.time.LocalDate.ofEpochDay(it) } }
    val heightCm: Flow<Int?> = context.dataStore.data.map { it[heightKey] }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[onboardingKey] ?: false }

    suspend fun saveProfile(name: String?, birthDate: java.time.LocalDate?, heightCm: Int?) {
        context.dataStore.edit { p ->
            if (name.isNullOrBlank()) p.remove(nameKey) else p[nameKey] = name.trim()
            if (birthDate == null) p.remove(dobKey) else p[dobKey] = birthDate.toEpochDay()
            if (heightCm == null) p.remove(heightKey) else p[heightKey] = heightCm
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[onboardingKey] = complete }
    }
}
