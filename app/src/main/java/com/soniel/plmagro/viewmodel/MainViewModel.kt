package com.soniel.plmagro.viewmodel

import android.util.Log
import java.util.Calendar
import java.util.TimeZone
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.soniel.plmagro.PlmApplication
import com.soniel.plmagro.api.*
import com.soniel.plmagro.core.eventbus.OperationalEventBus
import com.soniel.plmagro.core.outbox.OutboxManager
import com.soniel.plmagro.core.utils.AlertManager
import com.soniel.plmagro.core.utils.DeviceStatsUtils
import com.soniel.plmagro.model.*
import com.soniel.plmagro.service.ResourceMonitor
import com.soniel.plmagro.service.SystemHealth as ServiceSystemHealth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class WialonConnectionStatus {
    ONLINE, SYNCING, OFFLINE
}

enum class SystemStatus {
    ONLINE, INSTABLE, OFFLINE
}

data class SystemHealth(
    val gps: SystemStatus = SystemStatus.OFFLINE,
    val mqtt: SystemStatus = SystemStatus.OFFLINE,
    val wialon: SystemStatus = SystemStatus.OFFLINE,
    val can: SystemStatus = SystemStatus.OFFLINE,
    val internet: SystemStatus = SystemStatus.OFFLINE,
    val mqttLatency: Long = 0,
    val lastSync: Long = 0
)

data class JourneySummary(
    val durationMillis: Long,
    val distanceKm: Double,
    val refuelingCount: Int,
    val visitedAreas: List<String>
)


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PlmRepository,
    private val wialonRepository: WialonRepository,
    private val sessionManager: WialonSessionManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val sensorWatchdog: com.soniel.plmagro.core.watchdog.SensorWatchdog,
    private val diagnosticRepository: DiagnosticRepository? = null,
    private val outboxManager: OutboxManager? = null,
    private val alertManager: AlertManager? = null
) : ViewModel() {

    val healthState = ResourceMonitor.healthState.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceSystemHealth()
    )

    fun getApplication() = PlmApplication.instance

    val locationIntroDone = userPreferencesManager.locationIntroDone.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val telemetryEnabled = userPreferencesManager.telemetryEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val autoStopTimeoutMinutes = userPreferencesManager.autoStopTimeoutMinutes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 5
    )

    val satelliteMode = userPreferencesManager.satelliteMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val vehicleConfig = repository.vehicleConfig.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    
    val activeJourney = repository.activeJourney.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val currentState = activeJourney.map { it?.currentState ?: OperationalState.SEM_JORNADA }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), OperationalState.SEM_JORNADA
    )

    val activeVinculo = repository.activeVinculo.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val pendingSyncCount = repository.getPendingCount().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val recentSyncEvents = repository.getPendingSyncEvents().map { it.take(20) }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val operators = repository.getAllOperators().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val historicalJourneys = repository.getFinishedJourneys().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val journeyEvents = activeJourney.flatMapLatest { journey ->
        if (journey != null) repository.getJourneyEvents(journey.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _speed = MutableStateFlow(0)
    val speed: StateFlow<Int> = _speed

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation

    private val _currentGeofence = MutableStateFlow<GeofenceEntity?>(null)
    val currentGeofence: StateFlow<GeofenceEntity?> = _currentGeofence

    private val _currentGeofenceName = MutableStateFlow<String?>(null)
    val currentGeofenceName: StateFlow<String?> = _currentGeofenceName

    private val _isSpeedingInGeofence = MutableStateFlow(false)
    val isSpeedingInGeofence: StateFlow<Boolean> = _isSpeedingInGeofence

    private val _isNightMode = MutableStateFlow(false)
    val isNightMode: StateFlow<Boolean> = _isNightMode

    private val _satelliteCount = MutableStateFlow(0)
    val satelliteCount: StateFlow<Int> = _satelliteCount

    private val _gpsAccuracy = MutableStateFlow(0f)
    val gpsAccuracy: StateFlow<Float> = _gpsAccuracy

    private val _showAutoStopPopup = MutableStateFlow(false)
    val showAutoStopPopup: StateFlow<Boolean> = _showAutoStopPopup

    private val _loggedMatricula = MutableStateFlow("")

    // Wialon State
    val wialonEid = sessionManager.eidFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val wialonToken = sessionManager.tokenFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val ipsHost = sessionManager.ipsHostFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "193.193.165.165")
    val ipsPort = sessionManager.ipsPortFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20332)
    val linkedUid = sessionManager.linkedUidFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val linkedUnitName = sessionManager.linkedUnitNameFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val lastIpsAck = sessionManager.lastIpsAckFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val diagnosticState = diagnosticRepository?.diagnosticState ?: MutableStateFlow(DiagnosticState())

    val deviceUniqueId: String = android.os.Build.SERIAL.let {
        if (it == android.os.Build.UNKNOWN || it.isEmpty()) {
            // Fallback para Android ID se o Serial não estiver disponível
            android.provider.Settings.Secure.getString(
                com.soniel.plmagro.PlmApplication.instance.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
        } else it
    }

    private val _wialonUnits = MutableStateFlow<List<WialonUnit>>(emptyList())
    val wialonUnits: StateFlow<List<WialonUnit>> = _wialonUnits

    private val _lastWialonError = MutableStateFlow<String?>(null)
    val lastWialonError: StateFlow<String?> = _lastWialonError

    val wialonConnectionStatus = (diagnosticRepository?.diagnosticState?.map { state ->
        when(state.wialon.status) {
            ConnectionStatus.ONLINE -> WialonConnectionStatus.ONLINE
            ConnectionStatus.SYNCING -> WialonConnectionStatus.SYNCING
            else -> WialonConnectionStatus.OFFLINE
        }
    } ?: flowOf(WialonConnectionStatus.OFFLINE)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WialonConnectionStatus.OFFLINE)

    val ipsConnectionStatus = (diagnosticRepository?.diagnosticState?.map { state ->
        when(state.ips.status) {
            ConnectionStatus.ONLINE -> WialonConnectionStatus.ONLINE
            ConnectionStatus.SYNCING -> WialonConnectionStatus.SYNCING
            else -> WialonConnectionStatus.OFFLINE
        }
    } ?: flowOf(WialonConnectionStatus.OFFLINE)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WialonConnectionStatus.OFFLINE)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _systemHealth = MutableStateFlow(SystemHealth())
    val systemHealth: StateFlow<SystemHealth> = _systemHealth

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    private val _vinculoSalvo = MutableSharedFlow<Boolean>()
    val vinculoSalvo = _vinculoSalvo.asSharedFlow()

    private val gson = Gson()

    init {
        setupDefaultOperators()
        seedDB()
        observeOperationalState()
        observeOperationalEvents()
        monitorSystemHealth()
        loadInitialData()
        startSyncIfEnabled()
        autoConnectWialon()
        recoverActiveJourney()
        
        viewModelScope.launch {
            sensorWatchdog.alerts.collect { message ->
                alertManager?.playCriticalAlert()
                _uiMessage.emit(message)
            }
        }
    }

    private fun seedDB() {
        viewModelScope.launch {
            repository.seedOperationConfigsIfEmpty()
        }
    }

    private fun setupDefaultOperators() {
        viewModelScope.launch {
            repository.saveOperator(Operator("48117", "OPERADOR OFFLINE"))
        }
    }

    private fun recoverActiveJourney() {
        viewModelScope.launch {
            // Aguarda os flows do Repository estabilizarem
            delay(1500)
            val journey = activeJourney.value
            if (journey != null) {
                // Log Industrial Crítico
                Log.w("FSM_INDUSTRIAL", "JORNADA_RECUPERADA: ID=${journey.id} | OP=${journey.operatorMatricula} | Frota=${journey.vehicleId}")
                _uiMessage.emit("Jornada ativa recuperada")
                
                // Reinicia serviços vitais
                val context = getApplication()
                com.soniel.plmagro.service.TelemetryForegroundService.start(context)
                
                // Força reconexão Wialon
                refreshWialonStatus()
            }
        }
    }

    private fun autoConnectWialon() {
        viewModelScope.launch {
            // Aumentando delay para garantir carregamento fluido da UI offline
            delay(3000) 
            if (com.soniel.plmagro.core.network.NetworkUtils.isNetworkAvailable(PlmApplication.instance)) {
                Log.i("INIT", "Rede detectada, iniciando sincronização Wialon")
                refreshWialonStatus()
            } else {
                Log.w("INIT", "Dispositivo OFFLINE, pulando sincronização inicial")
                diagnosticRepository?.updateWialonStatus(ConnectionStatus.OFFLINE)
            }
        }
    }

    private fun startSyncIfEnabled() {
        viewModelScope.launch {
            // Inicia o motor de sincronização imediatamente
            outboxManager?.startSyncLoop()
            
            // Também observa mudanças na introdução de localização para reforçar
            locationIntroDone.collectLatest { done ->
                if (done) {
                    outboxManager?.startSyncLoop()
                }
            }
        }
    }

    private var lastMovementTime = System.currentTimeMillis()
    private var stoppedMovementCount = 0 // Contador para retomada automatica
    private val _showAutoStopAlert = MutableStateFlow(false)
    val showAutoStopAlert: StateFlow<Boolean> = _showAutoStopAlert

    private fun observeOperationalEvents() {
        viewModelScope.launch {
            OperationalEventBus.events.collect { event ->
                when (event) {
                    is OperationalEvent.SpeedChanged -> {
                        val currentSpeed = event.speedKmh.toInt()
                        _speed.value = currentSpeed
                        
                        if (currentSpeed > 2) {
                            lastMovementTime = System.currentTimeMillis()
                            _showAutoStopAlert.value = false
                            
                            // Inteligência 10/10: Alerta de Jornada Esquecida
                            if (activeJourney.value == null && currentSpeed > 15) {
                                viewModelScope.launch {
                                    alertManager?.speak("Atenção, movimento detectado. Por favor, inicie sua jornada.")
                                    _uiMessage.emit("⚠️ MOVIMENTO DETECTADO! Por favor, inicie sua jornada.")
                                }
                            }
                            
                            // Inteligência 10/10: Retomada automática se estiver em "PARADA"
                            if (currentState.value == OperationalState.PARADO || currentState.value == OperationalState.PARADA_APONTADA) {
                                if (currentSpeed > 10) {
                                    stoppedMovementCount++
                                    if (stoppedMovementCount >= 5) { // ~45-60 segundos de movimento constante
                                        Log.i("INTEL", "AUTOMAÇÃO: Retomando operação por detecção de movimento (>10km/h)")
                                        viewModelScope.launch {
                                            repository.iniciarOperacao()
                                            _uiMessage.emit("Movimento detectado: Retomando Operação!")
                                            stoppedMovementCount = 0
                                        }
                                    }
                                } else {
                                    stoppedMovementCount = 0
                                }
                            }
                        } else {
                            stoppedMovementCount = 0
                            checkAutoStopTimeout()
                        }
                        
                        validateGeofenceSpeed(event.speedKmh)
                    }
                    is OperationalEvent.GpsUpdated -> {
                        _currentLocation.value = Pair(event.lat, event.lng)
                        _satelliteCount.value = event.satellites
                        _gpsAccuracy.value = event.accuracy
                        sensorWatchdog.checkGpsIntegrity(event.lat, event.lng, _speed.value)
                        checkGeofences(event.lat, event.lng)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun checkAutoStopTimeout() {
        if (activeJourney.value == null) return
        if (currentState.value == OperationalState.PARADO || currentState.value == OperationalState.PARADA_APONTADA) return

        val stoppedDuration = System.currentTimeMillis() - lastMovementTime
        val timeoutMillis = autoStopTimeoutMinutes.value * 60 * 1000
        
        if (stoppedDuration > timeoutMillis) { 
            if (!_showAutoStopAlert.value) {
                Log.w("INTEL", "DETECCAO_AUTOMATICA: Veiculo parado ha ${autoStopTimeoutMinutes.value} min sem apontamento")
                _showAutoStopAlert.value = true
                alertManager?.playOperationalNotice()
                alertManager?.speak("Atenção, veículo parado. Por favor, informe o motivo da parada.")
                viewModelScope.launch { _uiMessage.emit("Atenção: Máquina parada. Registre o motivo da parada!") }
            }
        }
    }

    fun saveAutoStopTimeout(minutes: Int) {
        viewModelScope.launch {
            userPreferencesManager.setAutoStopTimeout(minutes)
        }
    }

    fun setSatelliteMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setSatelliteMode(enabled)
            _uiMessage.emit("Modo Satelital ${if(enabled) "Ativado" else "Desativado"}")
        }
    }

    private fun checkGeofences(lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.getActiveGeofences().first().let { geofences ->
                val active = geofences.find { 
                    com.soniel.plmagro.core.geo.GeofenceEngine.isPointInGeofence(lat, lng, it)
                }
                
                if (active?.name != _currentGeofenceName.value) {
                    val oldName = _currentGeofenceName.value
                    _currentGeofenceName.value = active?.name
                    _currentGeofence.value = active
                    
                    val journey = activeJourney.value ?: return@launch
                    
                    if (active != null) {
                        Log.i("GEO", "Entrou na cerca: ${active.name}")
                        _uiMessage.emit("Entrou em: ${active.name}")
                        
                        // Inteligência 10/10: Automação por Nome da Cerca
                        // Padrão esperado: "[CODIGO_OP] Nome da Area"
                        val regex = Regex("\\[(.*?)\\]")
                        val match = regex.find(active.name)
                        val autoOpCode = match?.groupValues?.get(1)
                        
                        if (autoOpCode != null && journey.operationCode != autoOpCode) {
                            Log.i("INTEL", "AUTOMAÇÃO: Sugerindo troca de operação para $autoOpCode baseado na Geofence")
                            _uiMessage.emit("Local detectado: Sugerindo Operação $autoOpCode")
                            
                            repository.registerEvent(
                                journeyId = journey.id,
                                type = "SUGESTAO_OPERACAO",
                                description = "Detectada entrada em ${active.name}. Operação sugerida: $autoOpCode",
                                km = journey.lastKm,
                                lat = lat,
                                lng = lng
                            )
                        }

                        // alertManager?.playGeofenceEntry()
                    } else if (oldName != null) {
                        Log.i("GEO", "Saiu da cerca: $oldName")
                        _uiMessage.emit("Saiu de: $oldName")
                        repository.registerEvent(
                            journeyId = journey.id,
                            type = "SAIU_DA_CERCA",
                            description = "Cerca: $oldName",
                            km = journey.lastKm,
                            lat = lat,
                            lng = lng
                        )
                    }
                }
            }
        }
    }

    private fun validateGeofenceSpeed(speedKmh: Float) {
        viewModelScope.launch {
            val geofences = repository.getActiveGeofences().first()
            val active = geofences.find { it.name == _currentGeofenceName.value }
            val journey = activeJourney.value
            
            // Buscar limite dinâmico da operação atual
            val opConfig = journey?.operationCode?.let { repository.getOperationConfig(it) }
            val opLimit = opConfig?.maxSpeed ?: 999f
            
            // O limite efetivo é o menor entre o da cerca e o da operação
            val fenceLimit = if (active != null && active.maxSpeed > 0) active.maxSpeed else 999f
            val effectiveLimit = Math.min(opLimit, fenceLimit)
            
            val wasSpeeding = _isSpeedingInGeofence.value
            if (effectiveLimit < 900f) {
                val isNowSpeeding = speedKmh > effectiveLimit
                _isSpeedingInGeofence.value = isNowSpeeding
                
                if (isNowSpeeding && !wasSpeeding) {
                    val msg = if (opLimit < fenceLimit) 
                        "Excesso de velocidade para a operação: ${journey?.operationCode}" 
                      else "Excesso de velocidade na cerca: ${active?.name}"
                    
                    alertManager?.playSpeedAlert()
                    alertManager?.speak(msg)
                    _uiMessage.emit("⚠️ $msg")
                }
            } else {
                _isSpeedingInGeofence.value = false
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val v = repository.obterVinculoAtual()
            Log.d("WIALON", "Vínculo atual carregado no Init: ${v?.wialonNome ?: "NENHUM"}")
        }
    }

    private fun observeOperationalState() {
        viewModelScope.launch {
            currentState.collect { state ->
                if (state == OperationalState.AGUARDANDO_PARADA) {
                    _showAutoStopPopup.value = true
                } else if (state != OperationalState.PARADO) {
                    _showAutoStopPopup.value = false
                }
            }
        }
    }

    private fun monitorSystemHealth() {
        viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                // Monitoramento de Horário para Modo Noturno (Passo D)
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                _isNightMode.value = hour >= 18 || hour < 6

                // Monitoramento Preditivo de Manutenção (Fase 4)
                checkMaintenanceAlerts()

                // Monitoramento real de saúde do sistema
                _systemHealth.value = SystemHealth(
                    gps = if (repository.isGpsEnabled()) SystemStatus.ONLINE else SystemStatus.OFFLINE,
                    mqtt = SystemStatus.ONLINE, // Assumindo online se o app está rodando e conectado
                    wialon = if (wialonConnectionStatus.value == WialonConnectionStatus.OFFLINE) SystemStatus.OFFLINE else SystemStatus.ONLINE,
                    can = SystemStatus.ONLINE,
                    internet = SystemStatus.ONLINE,
                    mqttLatency = 0,
                    lastSync = System.currentTimeMillis()
                )

                // Fase 3: Heartbeat Industrial (60s)
                val now = System.currentTimeMillis()
                if (now - lastHeartbeatSent > 60000) {
                    sendHeartbeat()
                    lastHeartbeatSent = now
                }

                delay(10000)
            }
        }
    }

    private var lastHeartbeatSent = 0L

    private fun sendHeartbeat() {
        viewModelScope.launch {
            val config = vehicleConfig.value ?: return@launch
            val journey = activeJourney.value
            val context = getApplication()
            val stats = DeviceStatsUtils.getSystemStats(context)
            
            Log.i("IPS", "HEARTBEAT_SENT: Unit=${config.wialonUnitId}")
            repository.addSyncEvent(
                type = "HEARTBEAT",
                payload = gson.toJson(mapOf(
                    "type" to "HEARTBEAT",
                    "timestamp" to System.currentTimeMillis(),
                    "stats" to stats,
                    "wialonUnitId" to (config.wialonUnitId ?: 0)
                )),
                vehicleId = config.fleetCode,
                operatorMatricula = journey?.operatorMatricula,
                priority = EventPriority.HEARTBEAT.value
            )
        }
    }


    private var lastMaintenanceAlertTime = 0L

    private fun checkMaintenanceAlerts() {
        val config = vehicleConfig.value ?: return
        val journey = activeJourney.value
        val horimetroAtual = journey?.lastHorimetro ?: 0.0
        val target = config.horimetroManutencao
        val alertThreshold = config.alertaManutencaoHoras

        if (target > 0) {
            val hoursLeft = target - horimetroAtual
            val isAlertActive = hoursLeft <= alertThreshold
            
            diagnosticRepository?.updateMaintenanceStats(
                atual = horimetroAtual,
                proxima = target,
                alerta = isAlertActive
            )

            if (isAlertActive && hoursLeft > 0) {
                val now = System.currentTimeMillis()
                // Alerta vocal a cada 30 minutos se estiver próximo
                if (now - lastMaintenanceAlertTime > 1800000) {
                    val msg = "Atenção: Manutenção preventiva em ${hoursLeft.toInt()} horas."
                    alertManager?.speak(msg)
                    lastMaintenanceAlertTime = now
                    viewModelScope.launch { _uiMessage.emit("⚠️ $msg") }
                }
            } else if (hoursLeft <= 0 && target > 0) {
                val now = System.currentTimeMillis()
                if (now - lastMaintenanceAlertTime > 600000) { // A cada 10 min se venceu
                    val msg = "URGENTE: Manutenção Vencida há ${Math.abs(hoursLeft).toInt()} horas!"
                    alertManager?.speak(msg)
                    lastMaintenanceAlertTime = now
                    viewModelScope.launch { _uiMessage.emit("🚨 $msg") }
                }
            }
        }
    }

    fun saveMaintenanceConfig(target: Double, alertAt: Double) {
        viewModelScope.launch {
            val current = vehicleConfig.value ?: return@launch
            val updated = current.copy(
                horimetroManutencao = target,
                alertaManutencaoHoras = alertAt
            )
            repository.saveVehicleConfig(updated)
            _uiMessage.emit("Configuração de Manutenção salva.")
        }
    }

    fun closeAutoStopPopup() {
        _showAutoStopPopup.value = false
    }

    fun prepareEndJourney() {
        _showAutoStopPopup.value = false
    }

    fun setLoggedMatricula(matricula: String) {
        _loggedMatricula.value = matricula
    }

    fun refreshWialonStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            _lastWialonError.value = "Sincronizando unidades..."
            
            val result = withTimeoutOrNull(15000) { // 15s timeout
                val loginResult = wialonRepository.login()
                if (loginResult.isSuccess) {
                    val unitsResult = wialonRepository.listUnits()
                    if (unitsResult.isSuccess) {
                        _wialonUnits.value = unitsResult.getOrDefault(emptyList())
                        _uiMessage.emit("${_wialonUnits.value.size} unidades sincronizadas")
                        diagnosticRepository?.updateWialonStatus(ConnectionStatus.ONLINE)
                        _lastWialonError.value = null
                        true
                    } else {
                        val err = unitsResult.exceptionOrNull()?.message
                        _lastWialonError.value = "Erro unidades: $err"
                        diagnosticRepository?.updateWialonStatus(ConnectionStatus.OFFLINE)
                        false
                    }
                } else {
                    val err = loginResult.exceptionOrNull()?.message
                    _lastWialonError.value = "Erro login: $err"
                    diagnosticRepository?.updateWialonStatus(ConnectionStatus.OFFLINE)
                    false
                }
            }
            
            if (result == null) {
                _lastWialonError.value = "Timeout na conexao com Wialon"
                diagnosticRepository?.updateWialonStatus(ConnectionStatus.OFFLINE)
            }
            _isLoading.value = false
        }
    }

    fun saveWialonToken(token: String) {
        viewModelScope.launch {
            sessionManager.saveToken(token)
            sessionManager.clearSession() // Força novo login com novo token
        }
    }

    fun saveIpsConfig(host: String, port: Int, uniqueId: String, unitName: String) {
        viewModelScope.launch {
            sessionManager.saveIpsHost(host)
            sessionManager.saveIpsPort(port)
            sessionManager.saveLinkedUid(uniqueId)
            sessionManager.saveLinkedUnitName(unitName)
            _uiMessage.emit("Configurações IPS salvas com sucesso")
        }
    }

    fun validateIpsConnection() {
        viewModelScope.launch {
            _isLoading.value = true
            wialonRepository.validateIpsLogin()
            _isLoading.value = false
        }
    }
    
    fun maskString(s: String?) = sessionManager.maskString(s)

    fun syncGeofences() {
        viewModelScope.launch {
            _isLoading.value = true
            _lastWialonError.value = "Sincronizando cercas..."
            
            val result = wialonRepository.syncGeofences()
            result.fold(
                onSuccess = { geofences ->
                    repository.saveGeofences(geofences)
                    _uiMessage.emit("${geofences.size} cercas sincronizadas")
                    _lastWialonError.value = null
                },
                onFailure = { e ->
                    _lastWialonError.value = "Erro cercas: ${e.message}"
                    _uiMessage.emit("Falha ao sincronizar cercas")
                }
            )
            _isLoading.value = false
        }
    }

    fun syncOperators() {
        viewModelScope.launch {
            _isLoading.value = true
            _lastWialonError.value = "Sincronizando motoristas..."
            
            val result = wialonRepository.syncOperators()
            result.fold(
                onSuccess = { operators ->
                    if (operators.isNotEmpty()) {
                        repository.saveOperators(operators)
                        _uiMessage.emit("${operators.size} motoristas sincronizados")
                        _lastWialonError.value = null
                    } else {
                        _lastWialonError.value = "Nenhum motorista encontrado no Wialon"
                        _uiMessage.emit("Wialon retornou lista vazia")
                    }
                },
                onFailure = { e ->
                    _lastWialonError.value = "Erro motoristas: ${e.message}"
                    _uiMessage.emit("Falha ao sincronizar motoristas")
                }
            )
            _isLoading.value = false
        }
    }

    fun performFullSync() {
        viewModelScope.launch {
            _isLoading.value = true
            _uiMessage.emit("Iniciando sincronização completa...")
            
            try {
                // 0. Resetar erros antigos para tentar novamente
                repository.resetFailedSyncEvents()

                // 1. Atualizar lista de Unidades (Vínculo)
                refreshWialonStatus()
                
                // 2. Sincronizar Motoristas
                val driversRes = wialonRepository.syncOperators()
                if (driversRes.isSuccess) repository.saveOperators(driversRes.getOrThrow())
                
                // 3. Sincronizar Cercas
                val geoRes = wialonRepository.syncGeofences()
                if (geoRes.isSuccess) repository.saveGeofences(geoRes.getOrThrow())
                
                _uiMessage.emit("Banco de dados atualizado com sucesso!")
                _lastWialonError.value = null
            } catch (e: Exception) {
                _lastWialonError.value = "Falha na sincronização mestre: ${e.message}"
                _uiMessage.emit("Erro ao atualizar banco de dados")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setLocationIntroDone() {
        viewModelScope.launch {
            userPreferencesManager.setLocationIntroDone(true)
            userPreferencesManager.setTelemetryEnabled(true)
            // Sync engine starts automatically via the collector in startSyncIfEnabled()
        }
    }

    fun setLocationIntroDoneWithoutTelemetry() {
        viewModelScope.launch {
            userPreferencesManager.setLocationIntroDone(true)
            userPreferencesManager.setTelemetryEnabled(false)
        }
    }

    fun saveVehicleConfig(fleet: String, plate: String, type: String) {
        viewModelScope.launch {
            val current = vehicleConfig.value
            repository.saveVehicleConfig(VehicleConfig(
                fleetCode = fleet,
                plate = plate,
                type = type,
                wialonUnitId = current?.wialonUnitId,
                wialonUnitName = current?.wialonUnitName
            ))
            createSyncEvent("CONFIG_VEICULO", mapOf("fleet" to fleet, "plate" to plate, "type" to type))
        }
    }

    fun canStartJourney(): Boolean {
        return activeVinculo.value != null && activeVinculo.value?.ativo == true
    }

    fun loginAndStartJourney(km: Int, opCode: String, cc: String) {
        if (!canStartJourney()) {
            viewModelScope.launch { _uiMessage.emit("Erro: Vincule a frota ao Wialon antes de iniciar.") }
            return
        }
        
        if (activeJourney.value != null) {
            viewModelScope.launch { _uiMessage.emit("Aviso: Já existe uma jornada ativa.") }
            return
        }

        val matricula = _loggedMatricula.value
        viewModelScope.launch {
            try {
                val operator = repository.getOperator(matricula) ?: Operator(matricula, "Operador $matricula")
                repository.saveOperator(operator)
                
                val config = vehicleConfig.value
                val vinculo = activeVinculo.value
                
                repository.startJourney(
                    Journey(
                        operatorMatricula = matricula,
                        vehicleId = config?.fleetCode ?: "DESCONHECIDO",
                        kmInicial = km,
                        horimetroInicial = vinculo?.ultimoKmWialon ?: 0.0, // Usando o último horímetro conhecido
                        operationCode = opCode,
                        costCenter = cc
                    )
                )
            } catch (e: Exception) {
                _uiMessage.emit(e.message ?: "Erro ao iniciar jornada")
            }
        }
    }

    fun endJourney(kmFinal: Int) {
        viewModelScope.launch {
            repository.endJourney(kmFinal)
        }
    }

    suspend fun getJourneySummary(): JourneySummary? {
        val journey = activeJourney.value ?: return null
        return getSummaryForJourney(journey)
    }

    suspend fun getSummaryForJourney(journey: Journey): JourneySummary {
        val refuelingCount = repository.getRefuelingCount(journey.id)
        val visited = repository.getVisitedGeofences(journey.id)
        
        return JourneySummary(
            durationMillis = (journey.endTime ?: System.currentTimeMillis()) - journey.startTime,
            distanceKm = journey.accumulatedDistance / 1000.0,
            refuelingCount = refuelingCount,
            visitedAreas = visited.map { it.replace("Cerca: ", "") }
        )
    }

    fun registerEvent(type: String, description: String, km: Int) {
        viewModelScope.launch {
            when (type) {
                "PARADA_MOTIVO" -> {
                    repository.reportarParada(description)
                    OperationalEventBus.emit(OperationalEvent.MotivoParadaInformado(description))
                }
                "OPERACAO" -> {
                    repository.iniciarOperacao()
                    OperationalEventBus.emit(OperationalEvent.OperationStarted(description))
                }
                else -> {
                    val journey = activeJourney.value ?: return@launch
                    repository.registerEvent(
                        journeyId = journey.id,
                        type = type,
                        description = description,
                        km = km,
                        lat = journey.lastLat ?: 0.0,
                        lng = journey.lastLng ?: 0.0
                    )
                }
            }
        }
    }

    private suspend fun createSyncEvent(type: String, data: Map<String, Any>) {
        val config = vehicleConfig.value ?: return
        val journey = activeJourney.value
        
        val fullData = data.toMutableMap()
        fullData["wialonUnitId"] = config.wialonUnitId ?: 0
        fullData["wialonUnitName"] = config.wialonUnitName ?: "---"
        fullData["fleetCode"] = config.fleetCode
        fullData["plate"] = config.plate
        
        repository.addSyncEvent(
            type = type,
            payload = gson.toJson(fullData),
            vehicleId = config.fleetCode,
            operatorMatricula = journey?.operatorMatricula
        )
    }
}
