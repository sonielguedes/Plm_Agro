package com.soniel.plmagro.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.soniel.plmagro.MainActivity
import com.soniel.plmagro.PlmApplication
import com.soniel.plmagro.core.eventbus.OperationalEventBus
import com.soniel.plmagro.core.fsm.MaquinaEstadoOperacional
import com.soniel.plmagro.core.watchdog.OperationalWatchdog
import com.soniel.plmagro.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TelemetryForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: PlmRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var fsm: MaquinaEstadoOperacional
    private lateinit var eventProcessor: EventProcessor
    private lateinit var watchdog: OperationalWatchdog
    
    private var activeJourney: Journey? = null
    private var lastGpsUpdate: Long = 0
    private var heartbeatJob: Job? = null
    private var satelliteCount = 0
    private var isPowerSaveMode = false

    private val gnssStatusCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                var usedCount = 0
                for (i in 0 until status.satelliteCount) {
                    if (status.usedInFix(i)) usedCount++
                }
                satelliteCount = usedCount
            }
        }
    } else null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { processLocation(it) }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "telemetry_service_channel"
        private const val TAG = "TelemetryService"
        private const val UPDATE_INTERVAL = 3000L // 3s
        private const val FASTEST_INTERVAL = 2000L // 2s

        fun start(context: Context) {
            // Safety check: Don't start if permissions are missing (prevents SecurityException)
            val fineLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
            val coarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            
            if (fineLocation != android.content.pm.PackageManager.PERMISSION_GRANTED && 
                coarseLocation != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Tentativa de iniciar serviço de telemetria sem permissões de localização. Abortando.")
                return
            }

            val intent = Intent(context, TelemetryForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TelemetryForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Iniciando TelemetryForegroundService")
        ResourceMonitor.incrementCollectors()
        
        repository = (application as PlmApplication).repository
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fsm = MaquinaEstadoOperacional(repository)
        eventProcessor = EventProcessor(repository, fsm, (application as PlmApplication).diagnosticRepository)
        
        val alertMgr = com.soniel.plmagro.core.utils.AlertManager(this)
        watchdog = OperationalWatchdog(repository, alertMgr)
        
        createNotificationChannel()
        startAsForeground()
        
        // Iniciar busca de GPS imediatamente ao criar o serviço
        requestLocationUpdates()
        
        observeJourney()
        watchdog.startMonitoring()
        startHeartbeat()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                val stats = com.soniel.plmagro.core.utils.DeviceStatsUtils.getSystemStats(this@TelemetryForegroundService)
                val battery = stats["batt"] as? Int ?: 100
                val charging = stats["charging"] as? Int ?: 0
                val temp = stats["temp"] as? Float ?: 0f
                val disk = stats["disk"] as? Long ?: 0L
                val totalDisk = stats["disk_total"] as? Long ?: 0L
                val signal = stats["signal"] as? Int ?: 0
                
                // Atualizar diagnóstico local (Independente de jornada)
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val gpsActive = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                
                (application as PlmApplication).diagnosticRepository.updateHardwareStats(
                    battery = battery,
                    charging = charging == 1,
                    temp = temp,
                    disk = disk,
                    totalDisk = totalDisk,
                    signal = signal,
                    gpsActive = gpsActive
                )

                val journey = activeJourney
                if (journey != null) {
                    // Lógica de Economia Extrema (Passo B)
                    val needsPowerSave = battery < 15 && charging == 0
                    if (needsPowerSave != isPowerSaveMode) {
                        isPowerSaveMode = needsPowerSave
                        Log.w(TAG, "Mudando modo de energia: PowerSave=$isPowerSaveMode")
                        requestLocationUpdates() // Reinicia GPS com novo intervalo
                    }

                    val payload = mapOf(
                        "journey" to journey,
                        "stats" to stats,
                        "type" to "HEARTBEAT",
                        "altitude" to 0.0,
                        "satellites" to satelliteCount
                    )
                    
                    Log.i(TAG, "HEARTBEAT_SENT: Unit=${journey.vehicleId} | Batt=$battery% | Signal=${stats["signal"]}")
                    repository.addSyncEvent(
                        type = "TELEMETRIA",
                        payload = com.google.gson.Gson().toJson(payload),
                        vehicleId = journey.vehicleId,
                        operatorMatricula = journey.operatorMatricula,
                        jornadaId = journey.id,
                        priority = 1
                    )
                }
                delay(60000) // 1 minute heartbeat
            }
        }
    }

    private fun startAsForeground() {
        val notification = createNotification("Monitoramento operacional ativo")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val interval = if (isPowerSaveMode) 30000L else UPDATE_INTERVAL
        val fastest = if (isPowerSaveMode) 15000L else FASTEST_INTERVAL

        // Cancela atualizações antigas antes de iniciar novas
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(fastest)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                gnssStatusCallback?.let { lm.registerGnssStatusCallback(it, Looper.getMainLooper().let { l -> android.os.Handler(l) }) }
            }

            Log.d(TAG, "Atualizações de localização solicitadas (Fused)")
            OperationalEventBus.tryEmit(OperationalEvent.WialonConnected) // GPS Status is essentially connected here
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao solicitar GPS Fused", e)
            OperationalEventBus.tryEmit(OperationalEvent.GpsError)
        }

        // Adicional: Verificar se o GPS está ligado no sistema
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.w(TAG, "GPS está desligado nas configurações do Android!")
            OperationalEventBus.tryEmit(OperationalEvent.GpsError)
        }
    }

    private fun processLocation(location: Location) {
        lastGpsUpdate = System.currentTimeMillis()
        val speedKmH = location.speed * 3.6f
        Log.d(TAG, "GPS FIX: ${location.latitude}, ${location.longitude} | Speed: $speedKmH | Sat: $satelliteCount")
        
        serviceScope.launch {
            // Emite para o barramento
            OperationalEventBus.emit(OperationalEvent.GpsUpdated(
                lat = location.latitude,
                lng = location.longitude,
                alt = location.altitude,
                accuracy = location.accuracy,
                heading = location.bearing,
                speed = speedKmH,
                satellites = satelliteCount
            ))
            
            OperationalEventBus.emit(OperationalEvent.SpeedChanged(speedKmH))
            
            // Atualiza notificação com estado real
            updateNotification("Velocidade: ${"%.1f".format(speedKmH)} km/h | Estado: ${fsm.currentState.value}")
        }
    }

    private fun observeJourney() {
        serviceScope.launch {
            repository.activeJourney.collect { journey ->
                val previousActive = activeJourney
                activeJourney = journey
                
                if (journey == null) {
                    updateNotification("Aguardando Início de Jornada")
                    if (previousActive != null) {
                        Log.d(TAG, "Jornada finalizada. Parando GPS.")
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            gnssStatusCallback?.let { lm.unregisterGnssStatusCallback(it) }
                        }
                    }
                } else {
                    OperationalEventBus.tryEmit(OperationalEvent.JornadaAtualizada(journey))
                    if (previousActive == null) {
                        Log.d(TAG, "Jornada iniciada. Reativando GPS.")
                        requestLocationUpdates()
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PLMAGRO Telemetria",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a telemetria operacional ativa em segundo plano"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PLMAGRO em operação")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Limpando TelemetryForegroundService")
        watchdog.stop()
        heartbeatJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            gnssStatusCallback?.let { lm.unregisterGnssStatusCallback(it) }
        }
        eventProcessor.stop()
        serviceScope.cancel()
        ResourceMonitor.decrementCollectors()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
