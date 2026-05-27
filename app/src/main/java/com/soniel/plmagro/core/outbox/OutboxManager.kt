package com.soniel.plmagro.core.outbox

import android.util.Log
import com.soniel.plmagro.api.WialonRepository
import com.soniel.plmagro.model.PlmDao
import com.soniel.plmagro.model.OutboxEventEntity
import com.soniel.plmagro.sync.OutboxSyncWorker
import androidx.work.*
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class OutboxManager @Inject constructor(
    private val plmDao: PlmDao,
    private val wialonRepository: WialonRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    fun enqueueOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "OneTimeIndustrialSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    fun startSyncLoop() {
        if (syncJob?.isActive == true) return
        
        syncJob = scope.launch {
            Log.d("OUTBOX", "Iniciando motor de sincronização industrial")
            
            // Loop de verificação de Comandos Remotos (Nikita)
            launch {
                while (isActive) {
                    wialonRepository.checkRemoteCommands()
                    delay(60000) // Produção: Verifica a cada 60 segundos
                }
            }

            plmDao.getPendingSyncEvents().collect { events ->
                if (events.isNotEmpty()) {
                    Log.d("OUTBOX", "Sincronizando ${events.size} eventos pendentes")
                    processEvents(events)
                }
            }
        }
    }

    private suspend fun processEvents(events: List<OutboxEventEntity>) {
        if (events.isEmpty()) return

        // 1. Filtrar eventos por Backoff Exponencial
        val now = System.currentTimeMillis()
        val eligibleEvents = events.filter { event ->
            if (event.lastAttempt == null) return@filter true
            // Backoff: 5s, 10s, 20s, 40s, 80s... Máximo 5 minutos.
            val backoffMillis = (2.0.pow(event.retryCount.toDouble()) * 5000).toLong().coerceAtMost(300000)
            now >= (event.lastAttempt + backoffMillis)
        }

        if (eligibleEvents.isEmpty()) return

        Log.i("IPS", "OUTBOX_REPLAY_START: ${eligibleEvents.size} eligible events")

        // 2. Compactação Heartbeat: Se houver muitos heartbeats, manter apenas o mais recente
        // para economizar socket/dados, mas garantindo telemetria completa.
        val heartbeats = eligibleEvents.filter { it.payloadJson.contains("\"type\":\"HEARTBEAT\"") }
        val heartbeatsToDiscard = if (heartbeats.size > 2) {
            heartbeats.sortedByDescending { it.timestamp }.drop(1)
        } else emptyList()

        if (heartbeatsToDiscard.isNotEmpty()) {
            heartbeatsToDiscard.forEach { plmDao.deleteSyncEvent(it) }
            Log.d("OUTBOX", "Compactação: Descartados ${heartbeatsToDiscard.size} heartbeats antigos.")
        }

        val finalEligible = eligibleEvents.filter { it !in heartbeatsToDiscard }
        
        // Priorizar PARADA_INDUSTRIAL para replay imediato e cronológico
        val priorityEvents = finalEligible.filter { it.tipoEvento == "PARADA_INDUSTRIAL" }.sortedBy { it.timestamp }
        val telemetryEvents = finalEligible.filter { it.tipoEvento == "TELEMETRIA" }.take(30)
        val otherEvents = finalEligible.filter { it.tipoEvento != "TELEMETRIA" && it.tipoEvento != "PARADA_INDUSTRIAL" }.take(10)

        // 2.5 Processar Paradas Industriais (Prioridade Máxima + Cronologia)
        if (priorityEvents.isNotEmpty()) {
            Log.i("OUTBOX", "PARADA_REPLAY_START: ${priorityEvents.size} events")
            for (event in priorityEvents) {
                val result = wialonRepository.syncOutboxEvent(event)
                if (result.isSuccess) {
                    plmDao.updateSyncEvent(event.copy(syncStatus = "ENVIADO", lastAttempt = System.currentTimeMillis(), enviadoEm = System.currentTimeMillis()))
                    Log.i("OUTBOX", "PARADA_REPLAY_SUCCESS: ${event.eventId}")
                } else {
                    handleFailure(event, result.exceptionOrNull()?.message ?: "Erro Sync Parada")
                    // Se falhar uma parada, interrompe a fila prioritária para manter cronologia estrita
                    break
                }
            }
        }

        // 3. Processar Telemetria em Lote (Batching)
        if (telemetryEvents.isNotEmpty()) {
            val result = wialonRepository.syncTelemetryBatch(telemetryEvents)
            if (result.isSuccess) {
                telemetryEvents.forEach { event ->
                    plmDao.updateSyncEvent(event.copy(syncStatus = "ENVIADO", lastAttempt = System.currentTimeMillis(), enviadoEm = System.currentTimeMillis()))
                }
                Log.d("OUTBOX", "OUTBOX_BATCH_SUCCESS: ${telemetryEvents.size} pts")
            } else {
                telemetryEvents.forEach { handleFailure(it, result.exceptionOrNull()?.message ?: "Erro IPS Batch") }
            }
        }

        // 4. Processar Eventos Críticos (Cronológico um a um)
        for (event in otherEvents) {
            if (!isActive()) break
            try {
                delay(500) // Estabilidade socket
                val result = wialonRepository.syncOutboxEvent(event)
                if (result.isSuccess) {
                    plmDao.updateSyncEvent(event.copy(syncStatus = "ENVIADO", lastAttempt = System.currentTimeMillis(), enviadoEm = System.currentTimeMillis()))
                } else {
                    handleFailure(event, result.exceptionOrNull()?.message ?: "Erro API")
                    break 
                }
            } catch (e: Exception) {
                handleFailure(event, e.message ?: "Exceção fatal")
                break
            }
        }
        
        Log.i("IPS", "OUTBOX_REPLAY_FINISH")
    }

    private suspend fun handleFailure(event: OutboxEventEntity, error: String) {
        val nextAttempt = event.retryCount + 1
        val isFinalFailure = nextAttempt >= 10 // Aumentado para resiliência industrial
        
        if (isFinalFailure) {
            Log.e("OUTBOX", "OUTBOX_EVENT_FAILED_FINAL: ${event.eventId} - Movendo para DLQ. Erro: $error")
            plmDao.moveToDeadLetter(event, error)
        } else {
            plmDao.updateSyncEvent(event.copy(
                syncStatus = "TENTANDO",
                retryCount = nextAttempt,
                lastAttempt = System.currentTimeMillis(),
                errorMessage = error
            ))
            Log.w("OUTBOX", "OUTBOX_EVENT_RETRY: ${event.eventId} (Tentativa $nextAttempt/10) - $error")
        }
    }

    private fun isActive() = scope.isActive

    fun stop() {
        scope.cancel()
    }
}
