package com.onesec.interceptor.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "onesec_settings")

data class InterceptorSettings(
    val interceptorEnabled: Boolean = true,
    val graceMinutes: Int = 5,
    val breathingSeconds: Int = 10
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val INTERCEPTOR_ENABLED = booleanPreferencesKey("interceptor_enabled")
        val GRACE_MINUTES = intPreferencesKey("grace_minutes")
        val BREATHING_SECONDS = intPreferencesKey("breathing_seconds")
    }

    val settingsFlow: Flow<InterceptorSettings> = context.dataStore.data.map { prefs ->
        InterceptorSettings(
            interceptorEnabled = prefs[Keys.INTERCEPTOR_ENABLED] ?: true,
            graceMinutes = prefs[Keys.GRACE_MINUTES] ?: 5,
            breathingSeconds = prefs[Keys.BREATHING_SECONDS] ?: 10
        )
    }

    suspend fun setInterceptorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INTERCEPTOR_ENABLED] = enabled }
    }

    suspend fun setGraceMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.GRACE_MINUTES] = minutes }
    }

    suspend fun setBreathingSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.BREATHING_SECONDS] = seconds }
    }
}
