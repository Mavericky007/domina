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
}
