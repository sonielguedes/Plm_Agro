package com.soniel.plmagro.model

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LOCATION_INTRO_DONE = booleanPreferencesKey("location_intro_done")
        private val TELEMETRY_ENABLED = booleanPreferencesKey("telemetry_enabled")
        private val AUTO_STOP_TIMEOUT_MINUTES = intPreferencesKey("auto_stop_timeout_minutes")
        private val LAST_GEOFENCE_SYNC = longPreferencesKey("last_geofence_sync")
        private val LAST_OPERATORS_SYNC = longPreferencesKey("last_operators_sync")
    }

    val locationIntroDone: Flow<Boolean> = context.userDataStore.data.map { it[LOCATION_INTRO_DONE] ?: false }
    val telemetryEnabled: Flow<Boolean> = context.userDataStore.data.map { it[TELEMETRY_ENABLED] ?: false }
    val autoStopTimeoutMinutes: Flow<Int> = context.userDataStore.data.map { it[AUTO_STOP_TIMEOUT_MINUTES] ?: 5 }
    val lastGeofenceSync: Flow<Long> = context.userDataStore.data.map { it[LAST_GEOFENCE_SYNC] ?: 0L }
    val lastOperatorsSync: Flow<Long> = context.userDataStore.data.map { it[LAST_OPERATORS_SYNC] ?: 0L }

    suspend fun setLocationIntroDone(done: Boolean) {
        context.userDataStore.edit { it[LOCATION_INTRO_DONE] = done }
    }

    suspend fun setTelemetryEnabled(enabled: Boolean) {
        context.userDataStore.edit { it[TELEMETRY_ENABLED] = enabled }
    }

    suspend fun setAutoStopTimeout(minutes: Int) {
        context.userDataStore.edit { it[AUTO_STOP_TIMEOUT_MINUTES] = minutes }
    }

    suspend fun updateGeofenceSyncTimestamp() {
        context.userDataStore.edit { it[LAST_GEOFENCE_SYNC] = System.currentTimeMillis() }
    }

    suspend fun updateOperatorsSyncTimestamp() {
        context.userDataStore.edit { it[LAST_OPERATORS_SYNC] = System.currentTimeMillis() }
    }
}
