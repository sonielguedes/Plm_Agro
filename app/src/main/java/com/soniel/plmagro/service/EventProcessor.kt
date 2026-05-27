package com.soniel.plmagro.service

import android.util.Log
import com.soniel.plmagro.model.*
import com.soniel.plmagro.core.eventbus.OperationalEventBus
import com.soniel.plmagro.core.fsm.MaquinaEstadoOperacional
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class EventProcessor(
    private val repository: PlmRepository,
    private val fsm: MaquinaEstadoOperacional,
    private val diagnosticRepository: DiagnosticRepository? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastLat: Double = 0.0
    private var lastLng: Double = 0.0
    private var lastKm: Int = 0
    private var stopStartTime: Long = 0
    private var isGpsError = false

    init {
        observeEvents()
    }

    private fun observeEvents() {
        ResourceMonitor.incrementCollectors()
        scope.launch {
            // Usamos collect em vez de collectLatest para garantir que 
            // a gravação no DB não seja cancelada por um novo evento de GPS
            OperationalEventBus.events.collect { event ->
                Log.d("EventProcessor", "Processing event: ${event::class.simpleName}")
                processEvent(event)
            }
        }
    }

    private suspend fun processEvent(event: OperationalEvent) {
        val activeJourney = repository.activeJourney.first()
        
        // 1. Update Diagnostics/Local State if needed
        when (event) {
            is OperationalEvent.GpsUpdated -> {
                if (isGpsError && activeJourney != null) {
                    repository.registerEvent(activeJourney.id, "RETORNO_GPS", "Sinal GPS restaurado", activeJourney.lastKm, event.lat, event.lng, 1)
                }
                isGpsError = false
                lastLat = event.lat
                lastLng = event.lng
                diagnosticRepository?.updateGpsStatus(ConnectionStatus.ONLINE)
                if (activeJourney != null) {
                    repository.updateJourneyTelemetry(
                        lat = event.lat,
                        lng = event.lng,
                        heading = event.heading,
                        speed = event.speed,
                        altitude = event.alt,
                        satellites = event.satellites
                    )
                }
            }
            is OperationalEvent.GpsLost, is OperationalEvent.GpsError -> {
                if (!isGpsError && activeJourney != null) {
                    repository.registerEvent(activeJourney.id, "PERDA_GPS", "Sinal GPS perdido ou instável", activeJourney.lastKm, lastLat, lastLng, 3)
                }
                isGpsError = true
                diagnosticRepository?.updateGpsStatus(ConnectionStatus.ERROR, error = "GPS Stalled/Hardware Error")
            }
            is OperationalEvent.MqttConnected -> {
                diagnosticRepository?.updateMqttStatus(ConnectionStatus.ONLINE)
            }
            is OperationalEvent.MqttDisconnected -> {
                diagnosticRepository?.updateMqttStatus(ConnectionStatus.OFFLINE)
            }
            is OperationalEvent.WialonConnected -> {
                diagnosticRepository?.updateWialonStatus(ConnectionStatus.ONLINE)
            }
            is OperationalEvent.WialonDisconnected -> {
                diagnosticRepository?.updateWialonStatus(ConnectionStatus.OFFLINE)
            }
            is OperationalEvent.SpeedChanged -> {
                // FSM will handle state transition
            }
            else -> {}
        }

        // 2. Apply FSM
        fsm.processEvent(event, activeJourney)

        // 3. Save History and generate Outbox for relevant events
        if (activeJourney != null) {
            // A gravação de MUDANÇA_ESTADO agora é feita automaticamente pelo PlmRepository.updateJourneyState
            // handleStateTransition(oldState, newState, activeJourney) // Removido para evitar duplicidade
            handlePersistence(event, activeJourney)
        }
    }

    private suspend fun handleStateTransition(old: OperationalState, new: OperationalState, journey: Journey) {
        // Função mantida vazia ou para lógicas que não envolvem log de histórico
    }

    private suspend fun handlePersistence(event: OperationalEvent, journey: Journey) {
        when (event) {
            is OperationalEvent.MotivoParadaInformado -> {
                repository.registerStopEvent(
                    journeyId = journey.id,
                    type = "PARADA_APONTADA",
                    motivo = event.reason,
                    inicio = stopStartTime,
                    fim = System.currentTimeMillis(),
                    km = journey.lastKm,
                    lat = lastLat,
                    lng = lastLng
                )
                stopStartTime = 0
            }
            is OperationalEvent.ParadaDetected, is OperationalEvent.ParadaDetectada -> {
                val duration = if (event is OperationalEvent.ParadaDetected) event.durationSeconds else (event as OperationalEvent.ParadaDetectada).durationSeconds
                stopStartTime = System.currentTimeMillis() - (duration * 1000)
                repository.registerStopEvent(
                    journeyId = journey.id,
                    type = "PARADA_INICIADA",
                    inicio = stopStartTime,
                    km = journey.lastKm,
                    lat = lastLat,
                    lng = lastLng
                )
            }
            is OperationalEvent.AbastecimentoRegistrado -> {
                repository.registerEvent(
                    journeyId = journey.id,
                    type = "ABASTECIMENTO",
                    description = "${event.liters}L at ${event.currentKm}km",
                    km = event.currentKm,
                    lat = lastLat,
                    lng = lastLng
                )
            }
            is OperationalEvent.OperacaoAlterada -> {
                repository.registerEvent(
                    journeyId = journey.id,
                    type = "OPERACAO_ALTERADA",
                    description = event.operation,
                    km = journey.lastKm,
                    lat = lastLat,
                    lng = lastLng
                )
            }
            else -> {}
        }
    }

    fun stop() {
        Log.d("EventProcessor", "Stopping EventProcessor and cancelling collectors")
        ResourceMonitor.decrementCollectors()
        scope.cancel()
    }
}
