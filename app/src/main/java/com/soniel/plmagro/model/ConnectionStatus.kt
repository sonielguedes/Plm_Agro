package com.soniel.plmagro.model

enum class ConnectionStatus {
    ONLINE,    // Green
    SYNCING,   // Yellow
    OFFLINE,   // Gray
    ERROR,     // Red
    AUTH_FAILED // Red
}

data class TechnicalStatus(
    val status: ConnectionStatus = ConnectionStatus.OFFLINE,
    val lastUpdate: Long = System.currentTimeMillis(),
    val latency: Long = 0,
    val errorMessage: String? = null
)

data class DiagnosticState(
    val gps: TechnicalStatus = TechnicalStatus(),
    val mqtt: TechnicalStatus = TechnicalStatus(),
    val wialon: TechnicalStatus = TechnicalStatus(),
    val ips: TechnicalStatus = TechnicalStatus(),
    val can: TechnicalStatus = TechnicalStatus(),
    val web: TechnicalStatus = TechnicalStatus(),
    val stopsToday: Int = 0,
    val pendingSync: Int = 0,
    val pendingTelemetry: Int = 0,
    val pendingEvents: Int = 0,
    val sentToday: Int = 0,
    val errorSync: Int = 0,
    val lastSyncTime: Long = 0,
    val apiLatency: Long = 0,
    val lastSyncError: String? = null,
    
    // Novas métricas IPS
    val lastIpsSentTime: Long = 0,
    val ipsTotalSent: Int = 0,
    val ipsTotalFailures: Int = 0,
    val ipsLastLatency: Long = 0,
    
    // Métricas Heartbeat/Hardware
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val batteryTemp: Float = 0f,
    val freeDiskMb: Long = 0,
    val totalDiskMb: Long = 0,
    val signalLevel: Int = 0,
    val lastHeartbeatTime: Long = 0,
    val gpsHardwareActive: Boolean = false
)
