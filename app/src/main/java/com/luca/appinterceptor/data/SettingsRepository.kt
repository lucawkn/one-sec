package com.luca.appinterceptor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val WATCHED = stringSetPreferencesKey("watched_apps")
        val COOLDOWN = intPreferencesKey("cooldown_minutes")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    /** Paketnamen der überwachten Apps. */
    val watchedApps: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.WATCHED] ?: emptySet() }

    /** Wie lange eine App nach "Ja, öffnen" nicht erneut unterbrochen wird. */
    val cooldownMinutes: Flow<Int> =
        context.dataStore.data.map { it[Keys.COOLDOWN] ?: 5 }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun toggleWatched(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.WATCHED] ?: emptySet()
            prefs[Keys.WATCHED] =
                if (packageName in current) current - packageName else current + packageName
        }
    }

    suspend fun setCooldownMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.COOLDOWN] = minutes.coerceIn(1, 120) }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }
}
