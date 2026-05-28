package com.soniel.plmagro.core.p2p

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.soniel.plmagro.core.utils.IndustrialLogger
import com.soniel.plmagro.model.OutboxEventEntity
import com.soniel.plmagro.model.PlmDao
import com.soniel.plmagro.model.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class P2pSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PlmDao,
    private val userPrefs: UserPreferencesManager,
    private val gson: Gson
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "com.soniel.plmagro.P2P_SYNC"
    
    private var isAdvertising = false
    private var isDiscovering = false
    private var currentHostEndpointId: String? = null

    fun startP2pEngine() {
        scope.launch {
            val isSupervisor = userPrefs.supervisorMode.first()
            if (isSupervisor) {
                startAdvertising()
            } else {
                startDiscovery()
            }
        }
    }

    fun stopP2pEngine() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        isAdvertising = false
        isDiscovering = false
        currentHostEndpointId = null
    }

    private fun startAdvertising() {
        if (isAdvertising) return
        
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startAdvertising("SUPERVISOR", SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener {
                isAdvertising = true
                IndustrialLogger.i("P2P", "Iniciou Advertising (Supervisor)")
            }
            .addOnFailureListener { e ->
                IndustrialLogger.e("P2P", "Falha ao iniciar Advertising", e)
            }
    }

    private fun startDiscovery() {
        if (isDiscovering) return
        
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener {
                isDiscovering = true
                IndustrialLogger.i("P2P", "Iniciou Discovery (Trator)")
            }
            .addOnFailureListener { e ->
                IndustrialLogger.e("P2P", "Falha ao iniciar Discovery", e)
            }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            IndustrialLogger.i("P2P", "Host encontrado: ${info.endpointName} ($endpointId)")
            // Conectar ao supervisor encontrado
            connectionsClient.requestConnection("TRATOR", endpointId, connectionLifecycleCallback)
                .addOnFailureListener { e ->
                    IndustrialLogger.e("P2P", "Falha ao pedir conexao com Host", e)
                }
        }

        override fun onEndpointLost(endpointId: String) {
            IndustrialLogger.w("P2P", "Host perdido: $endpointId")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            IndustrialLogger.i("P2P", "Conexão iniciada com: ${info.endpointName}")
            // Aceitar a conexão automaticamente
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    IndustrialLogger.i("P2P", "Conectado com sucesso ao endpoint: $endpointId")
                    scope.launch {
                        val isSupervisor = userPrefs.supervisorMode.first()
                        if (!isSupervisor) {
                            currentHostEndpointId = endpointId
                            sendPendingData(endpointId)
                        }
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    IndustrialLogger.w("P2P", "Conexão rejeitada por $endpointId")
                }
                else -> {
                    IndustrialLogger.e("P2P", "Erro de conexão: ${result.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            IndustrialLogger.w("P2P", "Desconectado de $endpointId")
            if (currentHostEndpointId == endpointId) {
                currentHostEndpointId = null
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes() ?: return
                val jsonStr = String(data)
                scope.launch {
                    try {
                        // Verifica se é ACK do Supervisor
                        if (jsonStr.startsWith("ACK:")) {
                            val eventId = jsonStr.substringAfter("ACK:")
                            dao.updateSyncStatus(eventId, "P2P_ENVIADO")
                            IndustrialLogger.i("P2P", "Recebeu ACK do supervisor para evento $eventId")
                            return@launch
                        }

                        // Caso contrário, o Supervisor está recebendo dados do Trator
                        val event = gson.fromJson(jsonStr, OutboxEventEntity::class.java)
                        
                        // Garante que a origem não seja reescrita se for importante, ou marca como via P2P
                        val p2pEvent = event.copy(origem = "P2P_TRATOR")
                        
                        // Insere no banco local do Supervisor
                        dao.insertOutboxEvent(p2pEvent)
                        IndustrialLogger.i("P2P", "Evento ${event.eventId} recebido via P2P e inserido no banco")

                        // Envia ACK de volta para o trator
                        connectionsClient.sendPayload(
                            endpointId,
                            Payload.fromBytes("ACK:${event.eventId}".toByteArray())
                        )
                    } catch (e: Exception) {
                        IndustrialLogger.e("P2P", "Erro ao processar payload: $jsonStr", e)
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private suspend fun sendPendingData(endpointId: String) {
        val pendingEvents = dao.getPendingOutboxEventsForP2p() // Vamos precisar criar isso no DAO
        if (pendingEvents.isEmpty()) {
            IndustrialLogger.i("P2P", "Sem eventos pendentes para transferir.")
            return
        }

        IndustrialLogger.i("P2P", "Iniciando transferência de ${pendingEvents.size} eventos pendentes.")
        
        for (event in pendingEvents) {
            val jsonStr = gson.toJson(event)
            connectionsClient.sendPayload(
                endpointId,
                Payload.fromBytes(jsonStr.toByteArray())
            )
            delay(100) // Pequeno delay pra não sobrecarregar
        }
    }
}
