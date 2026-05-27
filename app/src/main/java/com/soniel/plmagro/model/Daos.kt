package com.soniel.plmagro.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlmDao {
    @Query("SELECT * FROM vehicle_config LIMIT 1")
    fun getVehicleConfig(): Flow<VehicleConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVehicleConfig(config: VehicleConfig)

    @Query("SELECT * FROM operators WHERE matricula = :matricula LIMIT 1")
    suspend fun getOperator(matricula: String): Operator?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOperator(operator: Operator)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOperators(operators: List<Operator>)

    @Query("SELECT * FROM operators ORDER BY name ASC")
    fun getAllOperators(): Flow<List<Operator>>

    @Query("SELECT * FROM journeys WHERE isFinished = 0 LIMIT 1")
    fun getActiveJourney(): Flow<Journey?>

    @Insert
    suspend fun startJourney(journey: Journey): Long

    @Update
    suspend fun updateJourney(journey: Journey)

    @Insert
    suspend fun insertEvent(event: Event)

    @Query("SELECT * FROM events WHERE journeyId = :journeyId ORDER BY timestamp DESC")
    fun getEventsByJourney(journeyId: Long): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE isSynced = 0")
    suspend fun getUnsyncedEvents(): List<Event>

        // Outbox DAO Industrial
    @Query("SELECT * FROM sync_outbox WHERE syncStatus IN ('PENDENTE', 'ERRO', 'TENTANDO') AND retryCount < 10 ORDER BY prioridade DESC, criadoEm ASC")
    fun getPendingSyncEvents(): Flow<List<OutboxEventEntity>>

    @Query("SELECT * FROM sync_outbox WHERE syncStatus IN ('PENDENTE', 'ERRO', 'TENTANDO') AND retryCount < 10 ORDER BY prioridade DESC, criadoEm ASC LIMIT :limit")
    suspend fun getPendingSyncEventsList(limit: Int = 50): List<OutboxEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncEvent(event: OutboxEventEntity)

    @Update
    suspend fun updateSyncEvent(event: OutboxEventEntity)

    @Delete
    suspend fun deleteSyncEvent(event: OutboxEventEntity)

    @Query("DELETE FROM sync_outbox WHERE syncStatus = 'ENVIADO' AND timestamp < :timestamp")
    suspend fun deleteOldSyncedEvents(timestamp: Long)

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE syncStatus IN ('PENDENTE', 'TENTANDO', 'ERRO') AND retryCount < 10")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE syncStatus IN ('PENDENTE', 'TENTANDO', 'ERRO') AND retryCount < 10 AND tipoEvento = 'TELEMETRIA'")
    fun getPendingTelemetryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE syncStatus IN ('PENDENTE', 'TENTANDO', 'ERRO') AND retryCount < 10 AND tipoEvento != 'TELEMETRIA'")
    fun getPendingEventsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE syncStatus = 'ENVIADO' AND timestamp >= :startOfDay")
    fun getSentTodayCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE syncStatus = 'ERRO'")
    fun getErrorCount(): Flow<Int>

    @Query("SELECT errorMessage FROM sync_outbox WHERE syncStatus = 'ERRO' ORDER BY lastAttempt DESC LIMIT 1")
    fun getLastError(): Flow<String?>

    @Query("SELECT lastAttempt FROM sync_outbox WHERE syncStatus = 'ENVIADO' ORDER BY lastAttempt DESC LIMIT 1")
    fun getLastSyncTime(): Flow<Long?>

    // Dead Letter Queue
    @Insert
    suspend fun insertDeadLetter(event: DeadLetterEventEntity)

    @Transaction
    suspend fun moveToDeadLetter(event: OutboxEventEntity, reason: String) {
        val dlq = DeadLetterEventEntity(
            eventId = event.eventId,
            jornadaId = event.jornadaId,
            tipoEvento = event.tipoEvento,
            payloadJson = event.payloadJson,
            motivoFalha = reason,
            stacktrace = null,
            tentativas = event.retryCount,
            vehicleId = event.vehicleId
        )
        insertDeadLetter(dlq)
        deleteSyncEvent(event)
    }

    @Query("SELECT * FROM dead_letter_events ORDER BY horario DESC")
    fun getDeadLetters(): Flow<List<DeadLetterEventEntity>>

    // Telemetria History
    @Insert
    suspend fun insertTelemetria(telemetria: TelemetriaEntity)

    // Vinculo Frota Wialon
    @Query("SELECT * FROM vinculo_frota_wialon WHERE ativo = 1 ORDER BY atualizadoEm DESC LIMIT 1")
    fun getActiveVinculo(): Flow<VinculoFrotaWialonEntity?>

    @Query("SELECT * FROM vinculo_frota_wialon WHERE codigoFrotaLocal = :frota LIMIT 1")
    suspend fun getVinculoByFrota(frota: String): VinculoFrotaWialonEntity?

    @Query("SELECT * FROM vinculo_frota_wialon WHERE wialonUnitId = :unitId LIMIT 1")
    suspend fun getVinculoByUnitId(unitId: Long): VinculoFrotaWialonEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVinculo(vinculo: VinculoFrotaWialonEntity)

    @Update
    suspend fun updateVinculo(vinculo: VinculoFrotaWialonEntity)

    @Query("DELETE FROM vinculo_frota_wialon WHERE codigoFrotaLocal = :frota")
    suspend fun deleteVinculoByFrota(frota: String)

    @Query("SELECT COUNT(*) FROM events WHERE type IN ('PARADA_INICIADA', 'PARADA_APONTADA') AND timestamp >= :startOfDay")
    fun getStopsTodayCount(startOfDay: Long): Flow<Int>

    // Cercas Eletrônicas
    @Query("SELECT * FROM geofences WHERE active = 1")
    fun getActiveGeofences(): Flow<List<GeofenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofences(geofences: List<GeofenceEntity>)

    @Transaction
    suspend fun clearAndInsertGeofences(geofences: List<GeofenceEntity>) {
        clearGeofences()
        insertGeofences(geofences)
    }

    @Query("DELETE FROM geofences")
    suspend fun clearGeofences()

    @Query("DELETE FROM telemetria_history WHERE timestamp < :threshold")
    suspend fun deleteOldTelemetry(threshold: Long)

    @Query("DELETE FROM events WHERE timestamp < :threshold")
    suspend fun deleteOldEvents(threshold: Long)

    @Query("UPDATE sync_outbox SET syncStatus = 'PENDENTE', retryCount = 0 WHERE syncStatus = 'ERRO'")
    suspend fun resetFailedSyncEvents()

    @Query("SELECT * FROM journeys WHERE isFinished = 1 ORDER BY startTime DESC LIMIT 30")
    fun getFinishedJourneys(): Flow<List<Journey>>

    @Query("SELECT COUNT(*) FROM events WHERE journeyId = :journeyId AND type = 'ABASTECIMENTO'")
    suspend fun getRefuelingCount(journeyId: Long): Int

    @Query("SELECT DISTINCT description FROM events WHERE journeyId = :journeyId AND type = 'ENTROU_NA_CERCA'")
    suspend fun getVisitedGeofences(journeyId: Long): List<String>

    // Tabela Paradas Enterprise
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParada(parada: ParadaEntity)

    @Update
    suspend fun updateParada(parada: ParadaEntity)

    @Query("SELECT * FROM TabelaParadas WHERE uuid = :uuid LIMIT 1")
    suspend fun getParadaByUuid(uuid: String): ParadaEntity?

    @Query("SELECT * FROM TabelaParadas ORDER BY inicio DESC")
    fun getAllParadas(): Flow<List<ParadaEntity>>

    @Query("SELECT * FROM TabelaParadas WHERE syncStatus = 'PENDENTE'")
    suspend fun getPendingParadas(): List<ParadaEntity>

    @Query("SELECT * FROM TabelaParadas WHERE fim IS NULL ORDER BY inicio DESC LIMIT 1")
    suspend fun getUltimaParadaAberta(): ParadaEntity?

    @Transaction
    suspend fun insertParadaIndustrial(parada: ParadaEntity, outbox: OutboxEventEntity, event: Event) {
        insertParada(parada)
        insertSyncEvent(outbox)
        insertEvent(event)
    }

    // Configurações de Operação (Limites de Velocidade Dinâmicos)
    @Query("SELECT * FROM operation_configs WHERE operationCode = :code LIMIT 1")
    suspend fun getOperationConfig(code: String): OperationConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOperationConfig(config: OperationConfigEntity)

    @Query("SELECT COUNT(*) FROM operation_configs")
    suspend fun getOperationConfigsCount(): Int
}
