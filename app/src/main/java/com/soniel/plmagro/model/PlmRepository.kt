package com.soniel.plmagro.model

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.google.gson.Gson
import com.soniel.plmagro.core.eventbus.OperationalEventBus
import com.soniel.plmagro.core.utils.DeviceStatsUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

class PlmRepository(
    private val plmDao: PlmDao,
    private val context: Context
) {
    private val gson = Gson()

    private fun calculateSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    val vehicleConfig: Flow<VehicleConfig?> = plmDao.getVehicleConfig()
    val activeJourney: Flow<Journey?> = plmDao.getActiveJourney()
    val activeVinculo: Flow<VinculoFrotaWialonEntity?> = plmDao.getActiveVinculo()

    fun getJourneyEvents(journeyId: Long): Flow<List<Event>> = plmDao.getEventsByJourney(journeyId)

    suspend fun obterVinculoAtual(): VinculoFrotaWialonEntity? {
        return plmDao.getActiveVinculo().first()
    }

    fun getActiveGeofences(): Flow<List<GeofenceEntity>> = plmDao.getActiveGeofences()

    suspend fun saveGeofences(geofences: List<GeofenceEntity>) {
        plmDao.clearAndInsertGeofences(geofences)
    }

    suspend fun saveVehicleConfig(config: VehicleConfig) {
        plmDao.saveVehicleConfig(config)
    }

    suspend fun getRefuelingCount(journeyId: Long): Int = plmDao.getRefuelingCount(journeyId)
    suspend fun getVisitedGeofences(journeyId: Long): List<String> = plmDao.getVisitedGeofences(journeyId)

    fun getTotalStopDuration(journeyId: Long): Flow<Long?> = plmDao.getTotalStopDuration(journeyId)

    suspend fun getOperator(matricula: String): Operator? {
        return plmDao.getOperator(matricula)
    }

    suspend fun saveOperator(operator: Operator) {
        plmDao.saveOperator(operator)
    }

    suspend fun saveOperators(operators: List<Operator>) {
        plmDao.saveOperators(operators)
    }

    suspend fun resetFailedSyncEvents() {
        plmDao.resetFailedSyncEvents()
    }

    fun getAllOperators(): Flow<List<Operator>> = plmDao.getAllOperators()

    fun getFinishedJourneys(): Flow<List<Journey>> = plmDao.getFinishedJourneys()

    suspend fun getOperationConfig(code: String): OperationConfigEntity? {
        return plmDao.getOperationConfig(code)
    }

    suspend fun saveOperationConfig(config: OperationConfigEntity) {
        plmDao.saveOperationConfig(config)
    }

    suspend fun seedOperationConfigsIfEmpty() {
        if (plmDao.getOperationConfigsCount() == 0) {
            val defaults = listOf(
                OperationConfigEntity("PLANTIO", 8f, "Operação de Plantio"),
                OperationConfigEntity("COLHEITA", 6f, "Operação de Colheita"),
                OperationConfigEntity("PULVERIZACAO", 12f, "Operação de Pulverização"),
                OperationConfigEntity("DESLOCAMENTO", 40f, "Deslocamento em Estrada")
            )
            defaults.forEach { plmDao.saveOperationConfig(it) }
            Log.i("DB_SEED", "Seed de configurações de operação concluído.")
        }
    }

    suspend fun startJourney(journey: Journey) {
        // Validação: Deve haver um vínculo ativo
        val vinculo = activeVinculo.first() ?: throw IllegalStateException("Nao e possivel iniciar jornada sem veiculo vinculado.")
        val journeyId = plmDao.startJourney(journey)
        val payload = mapOf(
            "journey" to journey,
            "kmInicial" to journey.kmInicial,
            "operador" to journey.operatorMatricula,
            "frota" to vinculo.codigoFrotaLocal,
            "wialonUnitId" to vinculo.wialonUnitId
        )
        addSyncEvent("START_JOURNEY", gson.toJson(payload), journey.vehicleId, journey.operatorMatricula, journeyId, 1)
        OperationalEventBus.tryEmit(OperationalEvent.JourneyStarted(journey))
    }

    private val stateMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun updateJourneyState(state: OperationalState, description: String? = null) {
        stateMutex.withLock {
            val journey = activeJourney.first() ?: return
            if (journey.currentState == state) return
            
            val updated = journey.copy(currentState = state)
            plmDao.updateJourney(updated)
            
            val friendlyDescription = description ?: when(state) {
                OperationalState.OPERANDO -> "Início de Operação"
                OperationalState.PARADO -> "Veículo Parado"
                OperationalState.PARADA_APONTADA -> "Parada Informada"
                OperationalState.MANUTENCAO -> "Entrada em Manutenção"
                OperationalState.AGUARDANDO -> "Aguardando Logística"
                else -> state.name.replace("_", " ")
            }

            // FSM_STATE_CHANGED Log
            Log.i("FSM", "FSM_STATE_CHANGED: ${journey.currentState} -> $state")

            registerEvent(
                journeyId = journey.id, 
                type = "MUDANÇA_ESTADO", 
                description = "FSM: $friendlyDescription",
                km = journey.lastKm, 
                lat = journey.lastLat ?: 0.0, 
                lng = journey.lastLng ?: 0.0
            )
        }
    }

    /**
     * Implementação Industrial Enterprise: Apontamento de Parada com Integridade Total
     */
    suspend fun reportarParada(motivo: String) {
        val journey = activeJourney.first() ?: return
        val vinculo = activeVinculo.first()
        
        val lat = journey.lastLat ?: 0.0
        val lng = journey.lastLng ?: 0.0
        val km = journey.lastKm
        val horimetro = journey.lastHorimetro
        val operador = journey.operatorMatricula

        // Bloqueio de log industrial
        Log.w("FSM_ENTERPRISE", "PARADA_STARTED: Motivo=$motivo | UUID=${java.util.UUID.randomUUID()}")

        // Persistência com Retry e Transação
        var success = false
        var attempts = 0
        while (!success && attempts < 3) {
            try {
                persistirParadaAtomicamente(
                    tipo = motivo,
                    operador = operador,
                    lat = lat,
                    lon = lng,
                    km = km,
                    horimetro = horimetro,
                    journeyId = journey.id,
                    vehicleId = vinculo?.codigoFrotaLocal ?: "---",
                    wialonUnitId = vinculo?.wialonUnitId ?: 0L
                )
                success = true
                Log.d("ENTERPRISE_DB", "Parada persistida com sucesso na tentativa ${attempts + 1}")
            } catch (e: Exception) {
                attempts++
                Log.e("ENTERPRISE_DB", "Falha na persistência (tentativa $attempts): ${e.message}")
                if (attempts < 3) kotlinx.coroutines.delay(100 * attempts.toLong())
                else throw e // Recovery reboot lidará com falhas críticas via Outbox
            }
        }
        
        // 3. Atualizar Estado da FSM
        updateJourneyState(OperationalState.PARADA_APONTADA, "Parada: $motivo")
    }

    private suspend fun persistirParadaAtomicamente(
        tipo: String,
        operador: String,
        lat: Double,
        lon: Double,
        km: Int,
        horimetro: Double,
        journeyId: Long,
        vehicleId: String,
        wialonUnitId: Long
    ) {
        val uuid = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // 1. Entidade Parada
        val parada = ParadaEntity(
            uuid = uuid,
            jornadaId = journeyId,
            tipo = tipo,
            inicio = timestamp,
            operador = operador,
            lat = lat,
            lon = lon,
            km = km,
            horimetro = horimetro,
            syncStatus = "PENDENTE"
        )

        // 2. Entidade Evento (para histórico local)
        val event = Event(
            journeyId = journeyId,
            type = "PARADA_APONTADA",
            description = "MOTIVO: $tipo | UUID: $uuid",
            kmAtTime = km,
            horimetroAtTime = horimetro,
            latitude = lat,
            longitude = lon,
            timestamp = timestamp
        )

        // 3. Entidade Outbox (para sincronização)
        val payloadMap = mapOf(
            "uuid" to uuid,
            "tipo" to tipo,
            "timestamp" to timestamp,
            "operador" to operador,
            "lat" to lat,
            "lon" to lon,
            "km" to km,
            "horimetro" to horimetro,
            "frota" to vehicleId,
            "wialonUnitId" to wialonUnitId
        )
        val outbox = OutboxEventEntity(
            eventId = uuid, // Idempotência: eventId pareado com UUID da parada
            tipoEvento = "PARADA_INDUSTRIAL",
            payloadJson = gson.toJson(payloadMap),
            vehicleId = vehicleId,
            operatorMatricula = operador,
            jornadaId = journeyId,
            prioridade = 2,
            hashIntegridade = calculateSha256(gson.toJson(payloadMap))
        )

        // Persistência Atômica via Room @Transaction
        plmDao.insertParadaIndustrial(parada, outbox, event)
        
        Log.i("ENTERPRISE_DB", "Atomic transaction completed for UUID: $uuid")
    }

    suspend fun iniciarOperacao() {
        fecharParadaAberta()
        updateJourneyState(OperationalState.OPERANDO, "Retomada de Operação")
    }

    private suspend fun fecharParadaAberta() {
        val paradaAberta = plmDao.getUltimaParadaAberta() ?: return
        val vinculo = activeVinculo.first()
        val agora = System.currentTimeMillis()
        val duracaoSegundos = (agora - paradaAberta.inicio) / 1000
        
        val paradaFechada = paradaAberta.copy(
            fim = agora,
            duracao = duracaoSegundos,
            syncStatus = "PENDENTE"
        )
        
        plmDao.updateParada(paradaFechada)
        
        // Registrar evento de fechamento para sincronização
        val payload = mapOf(
            "uuid" to paradaAberta.uuid,
            "tipo" to paradaAberta.tipo,
            "inicio" to paradaAberta.inicio,
            "fim" to agora,
            "duracao" to duracaoSegundos,
            "status" to "FECHADA",
            "wialonUnitId" to (vinculo?.wialonUnitId ?: 0L)
        )
        
        addSyncEvent(
            type = "PARADA_FINALIZADA",
            payload = gson.toJson(payload),
            vehicleId = vinculo?.codigoFrotaLocal ?: paradaAberta.uuid,
            operatorMatricula = paradaAberta.operador,
            priority = 2
        )
        Log.i("FSM_ENTERPRISE", "PARADA_FINALIZADA: UUID=${paradaAberta.uuid} | Duracao=${duracaoSegundos}s")
    }

    suspend fun registerEvent(journeyId: Long, type: String, description: String, km: Int, lat: Double, lng: Double, severity: Int = 0) {
        val journey = activeJourney.first()
        val horimetro = journey?.lastHorimetro ?: 0.0
        
        val eventId = java.util.UUID.randomUUID().toString()
        val event = Event(
            journeyId = journeyId,
            type = type,
            description = description,
            kmAtTime = km,
            horimetroAtTime = horimetro,
            latitude = lat,
            longitude = lng,
            severity = severity,
            timestamp = System.currentTimeMillis()
        )
        plmDao.insertEvent(event)
        
        Log.d("PARADA", "Salva Room: $eventId (Tipo: $type | KM: $km | HOR: $horimetro)")

        val vinculo = activeVinculo.first()
        
        val payloadMap = mutableMapOf(
            "eventId" to eventId,
            "jornadaId" to journeyId,
            "tipoEvento" to type,
            "descricao" to description,
            "timestamp" to event.timestamp,
            "latitude" to lat,
            "longitude" to lng,
            "kmAtual" to km,
            "horimetro" to horimetro,
            "severity" to severity,
            "frota" to (vinculo?.codigoFrotaLocal ?: ""),
            "matricula" to (journey?.operatorMatricula ?: ""),
            "wialonUnitId" to (vinculo?.wialonUnitId ?: 0)
        )
        
        addSyncEvent(type, gson.toJson(payloadMap), journey?.vehicleId ?: "---", journey?.operatorMatricula, journey?.id, 2)
    }

    suspend fun registerStopEvent(
        journeyId: Long,
        type: String, // PARADA_INICIADA, PARADA_APONTADA
        motivo: String = "",
        inicio: Long,
        fim: Long = 0,
        km: Int,
        lat: Double,
        lng: Double
    ) {
        val eventId = java.util.UUID.randomUUID().toString()
        val vinculo = activeVinculo.first()
        val journey = activeJourney.first()

        val payloadMap = mutableMapOf(
            "eventId" to eventId,
            "jornadaId" to journeyId,
            "tipoEvento" to type,
            "motivoParada" to motivo,
            "inicio" to inicio,
            "fim" to fim,
            "duracaoSegundos" to if (fim > inicio) (fim - inicio) / 1000 else 0,
            "frota" to (vinculo?.codigoFrotaLocal ?: ""),
            "matricula" to (journey?.operatorMatricula ?: ""),
            "wialonUnitId" to (vinculo?.wialonUnitId ?: 0L),
            "latitude" to lat,
            "longitude" to lng,
            "kmAtual" to km,
            "statusSync" to OutboxStatus.PENDENTE
        )

        val event = Event(
            journeyId = journeyId,
            type = type,
            description = if (motivo.isNotEmpty()) "Motivo: $motivo" else "Duração: ${payloadMap["duracaoSegundos"]}s",
            kmAtTime = km,
            latitude = lat,
            longitude = lng,
            timestamp = System.currentTimeMillis()
        )
        plmDao.insertEvent(event)
        Log.d("PARADA", "Salva Room: $eventId ($type)")

        addSyncEvent(type, gson.toJson(payloadMap), journey?.vehicleId ?: "---", journey?.operatorMatricula, journey?.id, 2)
        Log.d("OUTBOX", "Pendente: $eventId")
    }

    // Sync Outbox methods
    fun getPendingCount(): Flow<Int> = plmDao.getPendingCount()

    fun getPendingSyncEvents(): Flow<List<OutboxEventEntity>> = plmDao.getPendingSyncEvents()
    
    suspend fun addSyncEvent(
        type: String, 
        payload: String, 
        vehicleId: String, 
        operatorMatricula: String?,
        jornadaId: Long? = null,
        priority: Int = 0
    ) {
        val event = OutboxEventEntity(
            tipoEvento = type,
            payloadJson = payload,
            vehicleId = vehicleId,
            operatorMatricula = operatorMatricula,
            jornadaId = jornadaId,
            prioridade = priority,
            hashIntegridade = calculateSha256(payload)
        )
        plmDao.insertSyncEvent(event)
    }

    suspend fun vincularFrota(vinculo: VinculoFrotaWialonEntity, force: Boolean = false): Result<Unit> {
        return try {
            Log.d("LINK_WIALON", "Repository: vincularFrota ${vinculo.codigoFrotaLocal} (force=$force)")
            val journey = activeJourney.first()
            
            if (journey != null) {
                if (force) {
                    Log.w("LINK_WIALON", "Encerrando jornada ativa forçadamente para novo vínculo")
                    endJourney(journey.lastKm)
                } else {
                    return Result.failure(IllegalStateException("Não é possível trocar frota com jornada ativa."))
                }
            }

            // 1. Check if unit is already used by ANOTHER fleet
            val existingByUnit = plmDao.getVinculoByUnitId(vinculo.wialonUnitId)
            if (existingByUnit != null && existingByUnit.codigoFrotaLocal != vinculo.codigoFrotaLocal) {
                return Result.failure(IllegalStateException("Unidade Wialon já vinculada à frota ${existingByUnit.codigoFrotaLocal}"))
            }

            // 2. Check if we already have a record for THIS fleet
            val existingByFrota = plmDao.getVinculoByFrota(vinculo.codigoFrotaLocal)
            
            if (existingByFrota != null) {
                // Update existing
                plmDao.updateVinculo(vinculo.copy(id = existingByFrota.id, atualizadoEm = System.currentTimeMillis()))
            } else {
                // Insert new
                plmDao.insertVinculo(vinculo)
            }
            
            // Update VehicleConfig (Dashboard source of truth)
            val currentConfig = vehicleConfig.first()
            val updatedConfig = if (currentConfig != null) {
                currentConfig.copy(
                    fleetCode = vinculo.codigoFrotaLocal,
                    plate = vinculo.placa,
                    type = vinculo.tipoVeiculo,
                    wialonUnitId = vinculo.wialonUnitId,
                    wialonUnitName = vinculo.wialonNome,
                    wialonUniqueId = vinculo.wialonUniqueId
                )
            } else {
                VehicleConfig(
                    fleetCode = vinculo.codigoFrotaLocal,
                    plate = vinculo.placa,
                    type = vinculo.tipoVeiculo,
                    wialonUnitId = vinculo.wialonUnitId,
                    wialonUnitName = vinculo.wialonNome,
                    wialonUniqueId = vinculo.wialonUniqueId
                )
            }
            saveVehicleConfig(updatedConfig)

            // Sync Event
            val eventType = if (existingByFrota != null) "VINCULO_WIALON_ALTERADO" else "VINCULO_WIALON_CRIADO"
            val syncData = mapOf(
                "wialonUnitId" to vinculo.wialonUnitId,
                "wialonUnitName" to vinculo.wialonNome,
                "operador" to vinculo.operadorResponsavel,
                "frotaLocal" to vinculo.codigoFrotaLocal,
                "placa" to vinculo.placa
            )
            addSyncEvent(eventType, gson.toJson(syncData), vinculo.codigoFrotaLocal, vinculo.operadorResponsavel, null, 1)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LINK_WIALON", "Erro ao salvar vínculo no Repository", e)
            Result.failure(e)
        }
    }

    suspend fun getVinculoByUnitId(unitId: Long): VinculoFrotaWialonEntity? {
        return plmDao.getVinculoByUnitId(unitId)
    }

    suspend fun updateJourneyTelemetry(lat: Double, lng: Double, heading: Float, speed: Float, altitude: Double = 0.0, satellites: Int = 0) {
        val journey = activeJourney.first() ?: return
        
        val now = System.currentTimeMillis()
        // Simplificação: Se estava operando, incrementa horímetro proporcional ao tempo (em horas)
        val timeDiffMillis = if (journey.lastLat != null) now - (journey.timestamp_ultima_atualizacao ?: now) else 0L
        val horimetroIncrement = if (journey.currentState == OperationalState.OPERANDO) {
            timeDiffMillis.toDouble() / 3600000.0 // converte ms para horas
        } else 0.0

        var distance = 0.0
        if (journey.lastLat != null && journey.lastLng != null) {
            val results = FloatArray(1)
            Location.distanceBetween(journey.lastLat, journey.lastLng, lat, lng, results)
            distance = results[0].toDouble()
            
            if (distance < 1.0) {
                distance = 0.0
            }
        }

        val newAccumulated = journey.accumulatedDistance + distance
        val updated = journey.copy(
            lastLat = lat,
            lastLng = lng,
            lastHeading = heading,
            lastSpeed = speed,
            accumulatedDistance = newAccumulated,
            lastKm = journey.kmInicial + (newAccumulated / 1000.0).toInt(),
            lastHorimetro = journey.lastHorimetro + horimetroIncrement,
            timestamp_ultima_atualizacao = now
        )
        
        Log.d("TELEMETRIA", "GPS: $lat, $lng | Speed: $speed | Hor: ${"%.4f".format(updated.lastHorimetro)} | KM: ${updated.lastKm}")

        plmDao.updateJourney(updated)

        // Industrial: High-resolution history
        plmDao.insertTelemetria(TelemetriaEntity(
            journeyId = updated.id,
            lat = lat,
            lng = lng,
            speed = speed,
            heading = heading,
            km = updated.lastKm,
            altitude = altitude,
            satellites = satellites
        ))
        
        // Every relevant GPS event should go to outbox (Phase 5 requirement 6)
        if (distance > 5.0 || Math.abs((journey.lastHeading ?: 0f) - heading) > 10f) {
             val deviceStats = DeviceStatsUtils.getSystemStats(context)
             val payload = mapOf(
                 "journey" to updated,
                 "stats" to deviceStats,
                 "altitude" to altitude,
                 "satellites" to satellites
             )
             addSyncEvent("TELEMETRIA", gson.toJson(payload), updated.vehicleId, updated.operatorMatricula, updated.id, 0)
        }
    }

    suspend fun endJourney(kmFinal: Int) {
        val journey = activeJourney.first() ?: return
        
        // Garantir que qualquer parada aberta seja fechada no fim do turno
        fecharParadaAberta()

        val updated = journey.copy(
            kmFinal = kmFinal,
            endTime = System.currentTimeMillis(),
            isFinished = true,
            currentState = OperationalState.FINALIZADA
        )
        plmDao.updateJourney(updated)
        val vinculo = activeVinculo.first()
        val payload = mapOf(
            "journey" to updated,
            "kmFinal" to kmFinal,
            "operador" to updated.operatorMatricula,
            "frota" to (vinculo?.codigoFrotaLocal ?: updated.vehicleId),
            "wialonUnitId" to (vinculo?.wialonUnitId ?: 0L)
        )
        addSyncEvent("END_JOURNEY", gson.toJson(payload), updated.vehicleId, updated.operatorMatricula, updated.id, 2)
        OperationalEventBus.tryEmit(OperationalEvent.JourneyFinished(updated))
    }

    fun isGpsEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}
