package com.soniel.plmagro.service

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

data class SystemHealth(
    val activeCollectors: Int = 0,
    val activeSockets: Int = 0,
    val memoryUsageMb: Long = 0,
    val gpsPrecision: Float = 0f,
    val isIpsOnline: Boolean = false,
    val pendingEvents: Int = 0,
    val batteryTemp: Float = 0f,
    val isCharging: Boolean = false
)

object ResourceMonitor {
    private val TAG = "ResourceMonitor"
    private val activeCollectors = AtomicInteger(0)
    private val activeSockets = AtomicInteger(0)
    
    private val _healthState = MutableStateFlow(SystemHealth())
    val healthState = _healthState.asStateFlow()

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun updateGpsPrecision(precision: Float) {
        _healthState.value = _healthState.value.copy(gpsPrecision = precision)
    }

    fun updateIpsStatus(online: Boolean) {
        _healthState.value = _healthState.value.copy(isIpsOnline = online)
    }

    fun updatePendingEvents(count: Int) {
        _healthState.value = _healthState.value.copy(pendingEvents = count)
    }

    fun updateHardwareStats(temp: Float, charging: Boolean) {
        _healthState.value = _healthState.value.copy(
            batteryTemp = temp,
            isCharging = charging
        )
        if (temp > 45f) Log.e(TAG, "ALERT: Bateria superaquecida: $temp°C")
    }

    fun incrementCollectors() {
        val count = activeCollectors.incrementAndGet()
        updateHealth()
        Log.d(TAG, "Collector added. Active: $count")
    }

    fun decrementCollectors() {
        val count = activeCollectors.decrementAndGet()
        updateHealth()
        Log.d(TAG, "Collector removed. Active: $count")
    }

    fun incrementSockets() {
        val count = activeSockets.incrementAndGet()
        updateHealth()
        Log.d(TAG, "Socket opened. Active: $count")
    }

    fun decrementSockets() {
        val count = activeSockets.decrementAndGet()
        updateHealth()
        Log.d(TAG, "Socket closed. Active: $count")
    }

    private fun updateHealth() {
        _healthState.value = _healthState.value.copy(
            activeCollectors = activeCollectors.get(),
            activeSockets = activeSockets.get(),
            memoryUsageMb = getMemoryUsage()
        )
    }

    fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                Log.i(TAG, "--- SYSTEM RESOURCE REPORT ---")
                Log.i(TAG, "Active Collectors: ${activeCollectors.get()}")
                Log.i(TAG, "Active Sockets: ${activeSockets.get()}")
                Log.i(TAG, "Memory Usage: ${getMemoryUsage()} MB")
                Log.i(TAG, "------------------------------")
                delay(60000) // Every minute
            }
        }
    }

    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
    }
}
