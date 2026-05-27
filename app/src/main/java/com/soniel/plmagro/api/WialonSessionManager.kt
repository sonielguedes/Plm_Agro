package com.soniel.plmagro.api

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.soniel.plmagro.core.security.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "wialon_prefs")

@Singleton
class WialonSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TOKEN_SECURE_KEY = "wialon_token_secure"
        private val EID_KEY = stringPreferencesKey("wialon_eid")
        private val BASE_URL_KEY = stringPreferencesKey("wialon_base_url")
        private val IPS_HOST_KEY = stringPreferencesKey("wialon_ips_host")
        private val IPS_PORT_KEY = intPreferencesKey("wialon_ips_port")
        private val LINKED_UID_KEY = stringPreferencesKey("wialon_linked_uid")
        private val LINKED_UNIT_NAME_KEY = stringPreferencesKey("wialon_linked_unit_name")
        private val LAST_IPS_ACK_KEY = stringPreferencesKey("wialon_last_ips_ack")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { 
        SecureStorage.getString(context, TOKEN_SECURE_KEY)
    }
    val eidFlow: Flow<String?> = context.dataStore.data.map { it[EID_KEY] }
    val baseUrlFlow: Flow<String> = context.dataStore.data.map { it[BASE_URL_KEY] ?: "https://hst-api.wialon.com/" }
    val ipsHostFlow: Flow<String> = context.dataStore.data.map { it[IPS_HOST_KEY] ?: "64.120.108.24" }
    val ipsPortFlow: Flow<Int> = context.dataStore.data.map { it[IPS_PORT_KEY] ?: 20332 }
    val linkedUidFlow: Flow<String?> = context.dataStore.data.map { it[LINKED_UID_KEY] }
    val linkedUnitNameFlow: Flow<String?> = context.dataStore.data.map { it[LINKED_UNIT_NAME_KEY] }
    val lastIpsAckFlow: Flow<String?> = context.dataStore.data.map { it[LAST_IPS_ACK_KEY] }

    suspend fun saveToken(token: String) {
        SecureStorage.saveString(context, TOKEN_SECURE_KEY, token)
    }

    suspend fun saveLinkedUid(uid: String?) {
        context.dataStore.edit {
            if (uid == null) it.remove(LINKED_UID_KEY) else it[LINKED_UID_KEY] = uid
        }
    }

    suspend fun saveLinkedUnitName(name: String?) {
        context.dataStore.edit {
            if (name == null) it.remove(LINKED_UNIT_NAME_KEY) else it[LINKED_UNIT_NAME_KEY] = name
        }
    }

    suspend fun saveLastIpsAck(ack: String?) {
        context.dataStore.edit {
            if (ack == null) it.remove(LAST_IPS_ACK_KEY) else it[LAST_IPS_ACK_KEY] = ack
        }
    }

    suspend fun saveEid(eid: String?) {
        context.dataStore.edit {
            if (eid == null) it.remove(EID_KEY) else it[EID_KEY] = eid
        }
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { it[BASE_URL_KEY] = url }
    }

    suspend fun saveIpsHost(host: String) {
        context.dataStore.edit { it[IPS_HOST_KEY] = host }
    }

    suspend fun saveIpsPort(port: Int) {
        context.dataStore.edit { it[IPS_PORT_KEY] = port }
    }

    suspend fun getToken(): String? = SecureStorage.getString(context, TOKEN_SECURE_KEY)
    suspend fun getEid(): String? = eidFlow.first()
    suspend fun getBaseUrl(): String = baseUrlFlow.first()

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(EID_KEY)
        }
    }

    fun maskString(input: String?): String {
        if (input == null) return "---"
        if (input.length <= 8) return "****"
        return input.take(4) + "...." + input.takeLast(4)
    }
}
