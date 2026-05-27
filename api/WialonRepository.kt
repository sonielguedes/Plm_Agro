package com.soniel.plmagro.api

import android.util.Log
import com.google.gson.Gson
import com.soniel.plmagro.model.SyncEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WialonRepository(private val baseUrl: String, private val token: String) {

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(WialonApiService::class.java)
    private var sessionId: String? = null

    suspend fun syncEvent(event: SyncEvent): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (sessionId == null) {
                login()
            }

            val sid = sessionId ?: return@withContext Result.failure(Exception("Sessão Wialon inválida"))
            
            // Mocking successful send for now as per instructions (Fase 2, item 1: integrating Wialon later but preparing layer)
            Log.d("WialonRepository", "Sincronizando evento: ${event.type} - Payload: ${event.payload}")
            
            // Simulação de chamada real
            // val params = Gson().toJson(mapOf("itemId" to event.vehicleId, "driverCode" to event.operatorMatricula))
            // val response = service.sendEvent(params = params, sessionId = sid)
            
            // if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Erro API: ${response.code()}"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WialonRepository", "Erro ao sincronizar", e)
            Result.failure(e)
        }
    }

    private suspend fun login() {
        val params = Gson().toJson(mapOf("token" to token))
        val response = service.login(params = params)
        if (response.isSuccessful) {
            sessionId = response.body()?.eid
        }
    }
}
