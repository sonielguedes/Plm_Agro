package com.soniel.plmagro.core.watchdog

import android.util.Log
import com.soniel.plmagro.core.eventbus.OperationalEventBus
import com.soniel.plmagro.model.OperationalEvent
import com.soniel.plmagro.model.PlmRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class OperationalWatchdog(
    private val repository: PlmRepository,
    private val alertManager: com.soniel.plmagro.core.utils.AlertManager? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastGpsTime = System.currentTimeMillis()
    private var isGpsLost = false
    private var watchdogJob: Job? = null

    fun startMonitoring() {
        if (watchdogJob?.isActive == true) return
        
        watchdogJob = scope.launch {
            Log.d("WATCHDOG", "Iniciando monitoramento operacional industrial")
            lastGpsTime = System.currentTimeMillis() // Reset ao iniciar
            
            // Monitorar barramento de eventos
            launch {
                OperationalEventBus.events.collectLatest { event ->
                    if (event is OperationalEvent.GpsUpdated) {
                        lastGpsTime = System.currentTimeMillis()
                        if (isGpsLost) {
                            Log.i("WATCHDOG", "GPS Recuperado")
                            OperationalEventBus.emit(OperationalEvent.GpsRecovered)
                            isGpsLost = false
                            registrarDiagnostico("GPS_RECOVERED", "Sinal GPS restaurado")
                        }
                    }
                }
            }

            // Loop de validação ativa (Watchdog)
            while (isActive) {
                val now = System.currentTimeMillis()
                val journey = repository.activeJourney.first()
                
                // Só alertar se houver uma jornada ativa (motorista trabalhando)
                if (journey != null) {
                    // Validação GPS (Stalled / Lost) - 2 minutos de tolerância
                    if (now - lastGpsTime > 120_000 && !isGpsLost) {
                        Log.e("WATCHDOG", "GPS Travado ou Sem Sinal (>120s)")
                        OperationalEventBus.emit(OperationalEvent.GpsLost)
                        isGpsLost = true
                        
                        // Alerta sonoro e visual para o motorista
                        alertManager?.playSpeedAlert() // Reuso do som de alerta
                        
                        registrarDiagnostico("GPS_LOST", "GPS parou de reportar durante jornada")
                    }
                }
                
                delay(10000) // Verificar a cada 10s
            }
        }
    }

    private suspend fun registrarDiagnostico(tipo: String, msg: String) {
        val journey = repository.activeJourney.first()
        repository.addSyncEvent(
            type = "DIAGNOSTICO",
            payload = "{\"tipo\":\"$tipo\", \"mensagem\":\"$msg\", \"timestamp\":${System.currentTimeMillis()}}",
            vehicleId = journey?.vehicleId ?: "---",
            operatorMatricula = journey?.operatorMatricula,
            jornadaId = journey?.id,
            priority = 1
        )
    }

    fun stop() {
        scope.cancel()
    }
}
