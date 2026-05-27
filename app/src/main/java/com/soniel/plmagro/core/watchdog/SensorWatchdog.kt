package com.soniel.plmagro.core.watchdog

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SensorWatchdog - Monitor de integridade dos sensores de campo.
 * Detecta se o GPS congelou ou se o hardware está operando fora dos limites.
 */
@Singleton
class SensorWatchdog @Inject constructor() {
    private val TAG = "Watchdog"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastUpdate = 0L
    private var frozenCount = 0

    private val _alerts = MutableSharedFlow<String>()
    val alerts = _alerts.asSharedFlow()

    private val _commands = MutableSharedFlow<String>()
    val commands = _commands.asSharedFlow()

    /**
     * Verifica se o GPS parou de enviar coordenadas diferentes enquanto o veículo deveria estar em movimento.
     */
    fun checkGpsIntegrity(lat: Double, lng: Double, speed: Int) {
        val now = System.currentTimeMillis()
        
        if (speed > 5) { // Se estiver andando mais de 5km/h
            if (lat == lastLat && lng == lastLng) {
                frozenCount++
                if (frozenCount >= 10) { // ~30-40 segundos de congelamento total
                    emitAlert("ALERTA: GPS CONGELADO detectado. Reiniciando sensor automaticamente...")
                    executeCommand("RESTART_GPS")
                    frozenCount = 0
                }
            } else {
                frozenCount = 0
            }
        }
        
        lastLat = lat
        lastLng = lng
        lastUpdate = now
    }

    private fun emitAlert(message: String) {
        scope.launch {
            Log.e(TAG, "WATCHDOG_ALERT: $message")
            _alerts.emit(message)
        }
    }

    private fun executeCommand(cmd: String) {
        scope.launch {
            Log.w(TAG, "WATCHDOG_COMMAND: $cmd")
            _commands.emit(cmd)
        }
    }
}
