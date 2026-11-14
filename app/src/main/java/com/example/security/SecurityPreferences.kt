package com.example.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_study_prefs")

class SecurityPreferences(private val context: Context) {

    val onboardingCompleted: Flow<Boolean> = context.securityDataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val biometricLockEnabled: Flow<Boolean> = context.securityDataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    val inactivityTimeoutSeconds: Flow<Long> = context.securityDataStore.data.map { prefs ->
        prefs[KEY_INACTIVITY_TIMEOUT_SEC] ?: 60L
    }

    fun observeReadingProgress(documentId: String): Flow<Int> {
        val key = intPreferencesKey("reading_page_$documentId")
        return context.securityDataStore.data.map { prefs -> prefs[key] ?: 0 }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setInactivityTimeoutSeconds(seconds: Long) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_INACTIVITY_TIMEOUT_SEC] = seconds
        }
    }

    suspend fun saveReadingProgress(documentId: String, pageIndex: Int) {
        val key = intPreferencesKey("reading_page_$documentId")
        context.securityDataStore.edit { prefs ->
            prefs[key] = pageIndex
        }
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val KEY_INACTIVITY_TIMEOUT_SEC = longPreferencesKey("inactivity_timeout_sec")
    }
}
