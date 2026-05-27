package com.soniel.plmagro.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticRepository @Inject constructor(private val plmDao: PlmDao? = null) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _diagnosticState = MutableStateFlow(DiagnosticState())
    val diagnosticState: StateFlow<DiagnosticState> = _diagnosticState.asStateFlow()

    init {
        plmDao?.let { dao ->
            scope.launch {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val stopsFlow = dao.getStopsTodayCount(startOfDay)
                val pendingFlow = dao.getPendingCount()
                val pendingTelemetryFlow = dao.getPendingTelemetryCount()
                val pendingEventsFlow = dao.getPendingEventsCount()
                val sentFlow = dao.getSentTodayCount(startOfDay)
                val errorFlow = dao.getErrorCount()
                val lastSyncFlow = dao.getLastSyncTime()
                val lastErrorFlow = dao.getLastError()

                combine(
                    stopsFlow, pendingFlow, pendingTelemetryFlow, pendingEventsFlow, sentFlow, errorFlow, lastSyncFlow, lastErrorFlow
                ) { args ->
                    val stops = args[0] as Int
                    val pending = args[1] as Int
                    val pendingTel = args[2] as Int
                    val pendingEvt = args[3] as Int
                    val sent = args[4] as Int
                    val errors = args[5] as Int
                    val lastSync = args[6] as Long?
                    val lastError = args[7] as String?

                    _diagnosticState.update { it.copy(
                        stopsToday = stops,
                        pendingSync = pending,
                        pendingTelemetry = pendingTel,
                        pendingEvents = pendingEvt,
                        sentToday = sent,
                        errorSync = errors,
                        lastSyncTime = lastSync ?: 0,
                        lastSyncError = lastError
                    ) }
                }.collect()
            }
        }
    }

    fun updateHardwareStats(
        battery: Int,
        charging: Boolean,
        temp: Float,
        disk: Long,
        totalDisk: Long = 0,
        signal: Int = 0,
        gpsActive: Boolean
    ) {
        _diagnosticState.update { it.copy(
            batteryLevel = battery,
            isCharging = charging,
            batteryTemp = temp,
            freeDiskMb = disk,
            totalDiskMb = totalDisk,
            signalLevel = signal,
            gpsHardwareActive = gpsActive,
            lastHeartbeatTime = System.currentTimeMillis()
        ) }
    }

    fun updateGpsStatus(status: ConnectionStatus, latency: Long = 0, error: String? = null) {
        _diagnosticState.update { it.copy(gps = TechnicalStatus(status, System.currentTimeMillis(), latency, error)) }
    }

    fun updateMqttStatus(status: ConnectionStatus, latency: Long = 0, error: String? = null) {
        _diagnosticState.update { it.copy(mqtt = TechnicalStatus(status, System.currentTimeMillis(), latency, error)) }
    }

    fun updateWialonStatus(status: ConnectionStatus, latency: Long = 0, error: String? = null) {
        _diagnosticState.update { it.copy(wialon = TechnicalStatus(status, System.currentTimeMillis(), latency, error)) }
    }

    fun updateIpsStatus(status: ConnectionStatus, latency: Long = 0, error: String? = null) {
        _diagnosticState.update { it.copy(
            ips = TechnicalStatus(status, System.currentTimeMillis(), latency, error),
            ipsLastLatency = if (latency > 0) latency else it.ipsLastLatency
        ) }
    }

    fun incrementIpsSent() {
        _diagnosticState.update { it.copy(
            ipsTotalSent = it.ipsTotalSent + 1,
            lastIpsSentTime = System.currentTimeMillis()
        ) }
    }

    fun incrementIpsFailure() {
        _diagnosticState.update { it.copy(ipsTotalFailures = it.ipsTotalFailures + 1) }
    }

    fun updateCanStatus(status: ConnectionStatus, latency: Long = 0, error: String? = null) {
        _diagnosticState.update { it.copy(can = TechnicalStatus(status, System.currentTimeMillis(), latency, error)) }
    }

    fun updateWebStatus(status: ConnectionStatus, latency: Long = 0, error: String? = null) {
        _diagnosticState.update { it.copy(web = TechnicalStatus(status, System.currentTimeMillis(), latency, error)) }
    }

    fun updateMaintenanceStats(atual: Double, proxima: Double, alerta: Boolean) {
        _diagnosticState.update { it.copy(
            horimetroAtual = atual,
            proximaManutencao = proxima,
            horasParaManutencao = (proxima - atual).coerceAtLeast(0.0),
            alertaManutencaoAtivo = alerta
        ) }
    }
}
