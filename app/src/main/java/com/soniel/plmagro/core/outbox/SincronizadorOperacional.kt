package com.soniel.plmagro.core.outbox

import android.util.Log
import com.soniel.plmagro.api.*
import com.soniel.plmagro.model.*
import kotlinx.coroutines.delay
import java.security.MessageDigest
import kotlin.math.pow

enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

class SincronizadorOperacional(
    private val plmDao: PlmDao,
    private val wialonRepository: WialonRepository
) {
    companion object {
        private const val TAG = "SYNC_ENGINE_INDUSTRIAL"
        private const val MAX_RETRIES = 5
        private const val BATCH_SIZE = 20
        private const val CIRCUIT_FAILURE_THRESHOLD = 3
        private const val CIRCUIT_COOLDOWN = 60000L // 1 min
    }

    private var circuitState = CircuitState.CLOSED
    private var failureCount = 0
    private var lastFailureTime = 0L

    suspend fun sincronizarBatch() {
        if (checkCircuitBreaker()) return

        val pendentes = plmDao.getPendingSyncEventsList(BATCH_SIZE)
        if (pendentes.isEmpty()) return

        Log.d(TAG, "Iniciando Batch Sync Industrial: ${pendentes.size} eventos")
        
        try {
            val gson = com.google.gson.Gson()
            val eventsPayload = pendentes.map { event ->
                val map = gson.fromJson(event.payloadJson, Map::class.java).toMutableMap()
                map["eventId"] = event.eventId
                map["hashIntegridade"] = event.hashIntegridade ?: ""
                map["tipoEvento"] = event.tipoEvento
                map as Map<String, Any>
            }

            val request = SyncBatchRequest(
                deviceId = android.os.Build.SERIAL, // Ou um ID persistente do tablet
                sessionId = java.util.UUID.randomUUID().toString(),
                batchId = java.util.UUID.randomUUID().toString(),
                events = eventsPayload
            )

            val result = wialonRepository.syncBatch(request)
            
            if (result.isSuccess) {
                val response = result.getOrNull()
                response?.results?.forEach { res ->
                    val originalEvent = pendentes.find { it.eventId == res.eventId }
                    if (originalEvent != null) {
                        when (res.status) {
                            "ACKNOWLEDGED", "DUPLICATED" -> onSuccess(originalEvent)
                            "INVALID_HASH" -> tratarErroFatal(originalEvent, "Hash Inválido no Servidor")
                            else -> onFailure(originalEvent, "Rejeitado pelo Servidor: ${res.status}")
                        }
                    }
                }
                failureCount = 0
                circuitState = CircuitState.CLOSED
            } else {
                val error = result.exceptionOrNull()?.message ?: "Erro de Rede"
                Log.e(TAG, "Falha no Batch: $error")
                pendentes.forEach { onFailure(it, error) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro crítico no processamento do Batch", e)
            pendentes.forEach { onFailure(it, e.message ?: "Erro desconhecido") }
        }
    }

    private fun checkCircuitBreaker(): Boolean {
        if (circuitState == CircuitState.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > CIRCUIT_COOLDOWN) {
                Log.i(TAG, "Circuit Breaker: Tentando recuperação (HALF_OPEN)")
                circuitState = CircuitState.HALF_OPEN
                return false
            }
            return true
        }
        return false
    }

    private suspend fun processarEvento(event: OutboxEventEntity) {
        try {
            // Validar Integridade SHA-256
            if (!validarHash(event)) {
                tratarErroFatal(event, "Falha de integridade SHA-256")
                return
            }

            val result = wialonRepository.syncOutboxEvent(event)
            
            if (result.isSuccess) {
                onSuccess(event)
            } else {
                onFailure(event, result.exceptionOrNull()?.message ?: "Erro API")
            }
        } catch (e: Exception) {
            onFailure(event, e.message ?: "Exceção técnica")
        }
    }

    private fun validarHash(event: OutboxEventEntity): Boolean {
        val calculated = calculateSha256(event.payloadJson)
        return event.hashIntegridade == calculated
    }

    private fun calculateSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private suspend fun onSuccess(event: OutboxEventEntity) {
        plmDao.updateSyncEvent(event.copy(
            syncStatus = "ENVIADO",
            lastAttempt = System.currentTimeMillis(),
            enviadoEm = System.currentTimeMillis()
        ))
        failureCount = 0
        circuitState = CircuitState.CLOSED
        Log.d(TAG, "✓ ACK: ${event.eventId}")
    }

    private suspend fun onFailure(event: OutboxEventEntity, error: String) {
        failureCount++
        if (failureCount >= CIRCUIT_FAILURE_THRESHOLD) {
            circuitState = CircuitState.OPEN
            lastFailureTime = System.currentTimeMillis()
            Log.e(TAG, "✖ Circuit Breaker ABERTO devido a falhas consecutivas.")
        }

        val nextAttempt = event.retryCount + 1
        if (nextAttempt >= MAX_RETRIES) {
            tratarErroFatal(event, error)
        } else {
            val backoff = (2.0.pow(nextAttempt.toDouble()) * 60000).toLong() // Real backoff em minutos (simulado ms p/ teste)
            plmDao.updateSyncEvent(event.copy(
                syncStatus = "TENTANDO",
                retryCount = nextAttempt,
                lastAttempt = System.currentTimeMillis(),
                errorMessage = error
            ))
            Log.w(TAG, "⚠ Retry ${nextAttempt}/${MAX_RETRIES}: ${event.eventId}. Backoff: ${backoff/1000}s")
            delay(1000) // Pequeno delay no loop, o backoff real é controlado pelo worker/scheduler
        }
    }

    private suspend fun tratarErroFatal(event: OutboxEventEntity, error: String) {
        Log.e(TAG, "✖ DEAD LETTER: ${event.eventId} - $error")
        
        // Mover para Dead Letter Queue
        plmDao.insertDeadLetter(DeadLetterEventEntity(
            eventId = event.eventId,
            jornadaId = event.jornadaId,
            tipoEvento = event.tipoEvento,
            payloadJson = event.payloadJson,
            motivoFalha = error,
            stacktrace = null,
            tentativas = event.retryCount + 1,
            vehicleId = event.vehicleId
        ))
        
        // Remover da Outbox principal
        plmDao.deleteSyncEvent(event)
    }
}
