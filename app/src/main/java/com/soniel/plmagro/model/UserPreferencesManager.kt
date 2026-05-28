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
        private val SATELLITE_MODE = booleanPreferencesKey("satellite_mode")
        private val SUPERVISOR_MODE = booleanPreferencesKey("supervisor_mode")
        private val CANBUS_MODE = androidx.datastore.preferences.core.stringPreferencesKey("canbus_mode")
        private val CANBUS_BT_MAC = androidx.datastore.preferences.core.stringPreferencesKey("canbus_bt_mac")
        private val CANBUS_USB_PORT = androidx.datastore.preferences.core.stringPreferencesKey("canbus_usb_port")
        private val ERP_API_URL = androidx.datastore.preferences.core.stringPreferencesKey("erp_api_url")
    }

    val locationIntroDone: Flow<Boolean> = context.userDataStore.data.map { it[LOCATION_INTRO_DONE] ?: false }
    val telemetryEnabled: Flow<Boolean> = context.userDataStore.data.map { it[TELEMETRY_ENABLED] ?: false }
    val autoStopTimeoutMinutes: Flow<Int> = context.userDataStore.data.map { it[AUTO_STOP_TIMEOUT_MINUTES] ?: 5 }
    val lastGeofenceSync: Flow<Long> = context.userDataStore.data.map { it[LAST_GEOFENCE_SYNC] ?: 0L }
    val lastOperatorsSync: Flow<Long> = context.userDataStore.data.map { it[LAST_OPERATORS_SYNC] ?: 0L }
    val satelliteMode: Flow<Boolean> = context.userDataStore.data.map { it[SATELLITE_MODE] ?: false }
    val supervisorMode: Flow<Boolean> = context.userDataStore.data.map { it[SUPERVISOR_MODE] ?: false }
    
    val canBusMode: Flow<String> = context.userDataStore.data.map { it[CANBUS_MODE] ?: "SIMULATED" }
    val canBusBtMac: Flow<String> = context.userDataStore.data.map { it[CANBUS_BT_MAC] ?: "" }
    val canBusUsbPort: Flow<String> = context.userDataStore.data.map { it[CANBUS_USB_PORT] ?: "/dev/ttyUSB0" }
    val erpApiUrl: Flow<String> = context.userDataStore.data.map { it[ERP_API_URL] ?: "" }

    suspend fun setLocationIntroDone(done: Boolean) {
        context.userDataStore.edit { it[LOCATION_INTRO_DONE] = done }
    }

    suspend fun setTelemetryEnabled(enabled: Boolean) {
        context.userDataStore.edit { it[TELEMETRY_ENABLED] = enabled }
    }

    suspend fun setAutoStopTimeout(minutes: Int) {
        context.userDataStore.edit { it[AUTO_STOP_TIMEOUT_MINUTES] = minutes }
    }

    suspend fun setSatelliteMode(enabled: Boolean) {
        context.userDataStore.edit { it[SATELLITE_MODE] = enabled }
    }

    suspend fun setSupervisorMode(enabled: Boolean) {
        context.userDataStore.edit { it[SUPERVISOR_MODE] = enabled }
    }

    suspend fun saveCanBusConfig(mode: String, btMac: String, usbPort: String) {
        context.userDataStore.edit { prefs ->
            prefs[CANBUS_MODE] = mode
            prefs[CANBUS_BT_MAC] = btMac
            prefs[CANBUS_USB_PORT] = usbPort
        }
    }

    suspend fun setErpApiUrl(url: String) {
        context.userDataStore.edit { it[ERP_API_URL] = url }
    }

    suspend fun updateGeofenceSyncTimestamp() {
        context.userDataStore.edit { it[LAST_GEOFENCE_SYNC] = System.currentTimeMillis() }
    }

    suspend fun updateOperatorsSyncTimestamp() {
        context.userDataStore.edit { it[LAST_OPERATORS_SYNC] = System.currentTimeMillis() }
    }
}
