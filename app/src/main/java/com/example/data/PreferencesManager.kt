package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.jadwalNihDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jadwalnih_prefs",
)

data class UserPreferences(
    val isDarkMode: Boolean = false,
    val lastCustomFocusMinutes: Int = 25,
    val hasSeededInitialData: Boolean = false,
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LAST_FOCUS_MINUTES = intPreferencesKey("last_focus_minutes")
        val HAS_SEEDED_INITIAL = booleanPreferencesKey("has_seeded_initial")
    }

    val preferencesFlow: Flow<UserPreferences> = context.jadwalNihDataStore.data.map { prefs ->
        UserPreferences(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: false,
            lastCustomFocusMinutes = (prefs[Keys.LAST_FOCUS_MINUTES] ?: 25).coerceIn(1, 240),
            hasSeededInitialData = prefs[Keys.HAS_SEEDED_INITIAL] ?: false,
        )
    }

    suspend fun getPreferencesOnce(): UserPreferences = preferencesFlow.first()

    suspend fun setDarkMode(enabled: Boolean) {
        context.jadwalNihDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun setLastFocusMinutes(minutes: Int) {
        context.jadwalNihDataStore.edit { prefs ->
            prefs[Keys.LAST_FOCUS_MINUTES] = minutes.coerceIn(1, 240)
        }
    }

    suspend fun setHasSeededInitialData(seeded: Boolean) {
        context.jadwalNihDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED_INITIAL] = seeded
        }
    }
}
