package com.soniel.plmagro.api

import android.util.Log
import com.google.gson.Gson
import com.soniel.plmagro.model.OutboxEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErpRepository @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "ErpRepository"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun sendEventToErp(event: OutboxEventEntity, erpUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (erpUrl.isBlank()) {
            // Se a URL estiver em branco, significa que o ERP não foi configurado.
            // Retornamos true para não ficar tentando eternamente.
            return@withContext true
        }

        try {
            // Decodifica o payloadJson interno para embutir nos 'dados'
            val dadosObj = try {
                gson.fromJson(event.payloadJson, Map::class.java)
            } catch(e: Exception) {
                mapOf("raw" to event.payloadJson)
            }

            // Construção do JSON Genérico
            val payload = mapOf(
                "eventId" to event.eventId,
                "codigoFrota" to event.vehicleId,
                "matriculaOperador" to event.operatorMatricula,
                "timestamp" to event.criadoEm,
                "tipoEvento" to event.tipoEvento,
                "dados" to dadosObj
            )

            val jsonBody = gson.toJson(payload)

            val request = Request.Builder()
                .url(erpUrl)
                .post(jsonBody.toRequestBody(JSON))
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.i(TAG, "Evento ${event.eventId} enviado com sucesso para o ERP.")
                response.close()
                return@withContext true
            } else {
                Log.e(TAG, "Falha ao enviar evento ${event.eventId} para o ERP. HTTP: ${response.code}")
                response.close()
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao enviar evento para o ERP: ${e.message}")
            return@withContext false
        }
    }
}
