package com.soniel.plmagro.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class EventPriority(val value: Int) {
    CRITICO(4),
    OPERACIONAL(3),
    TELEMETRIA(2),
    HEARTBEAT(1)
}

object OutboxStatus {
    const val PENDENTE = "PENDENTE"
    const val TENTANDO = "TENTANDO"
    const val ENVIADO = "ENVIADO"
    const val ERRO = "ERRO"
}

@Entity(tableName = "vehicle_config")
data class VehicleConfig(
    @PrimaryKey val id: Int = 1,
    val fleetCode: String,
    val plate: String,
    val type: String,
    val wialonUnitId: Long? = null,
    val wialonUnitName: String? = null,
    val wialonUniqueId: String? = null, // IMEI ou ID Único para Wialon IPS
    val horimetroManutencao: Double = 0.0, // Horas da próxima manutenção
    val alertaManutencaoHoras: Double = 50.0 // Alerta antecipado (ex: 50h antes)
)

@Entity(tableName = "operators")
data class Operator(
    @PrimaryKey val matricula: String,
    val name: String
)

@Entity(tableName = "journeys")
data class Journey(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operatorMatricula: String,
    val vehicleId: String,
    val kmInicial: Int,
    val kmFinal: Int? = null,
    val horimetroInicial: Double = 0.0,
    val operationCode: String,
    val costCenter: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val isFinished: Boolean = false,
    val currentState: OperationalState = OperationalState.JORNADA_ATIVA,
    val lastKm: Int = kmInicial,
    val lastHorimetro: Double = 0.0,
    val timestamp_ultima_atualizacao: Long? = null,
    val lastLat: Double? = null,
    val lastLng: Double? = null,
    val lastHeading: Float? = null,
    val lastSpeed: Float? = null,
    val accumulatedDistance: Double = 0.0
)

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val type: String,
    val description: String,
    val kmAtTime: Int,
    val horimetroAtTime: Double = 0.0,
    val latitude: Double,
    val longitude: Double,
    val severity: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "sync_outbox")
data class OutboxEventEntity(
    @PrimaryKey val eventId: String = UUID.randomUUID().toString(),
    val jornadaId: Long? = null,
    val tipoEvento: String,
    val payloadJson: String,
    val syncStatus: String = OutboxStatus.PENDENTE,
    val retryCount: Int = 0,
    val lastAttempt: Long? = null,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val vehicleId: String,
    val operatorMatricula: String? = null,
    val prioridade: Int = EventPriority.OPERACIONAL.value,
    val origem: String = "APK",
    val hashIntegridade: String? = null,
    val criadoEm: Long = System.currentTimeMillis(),
    val enviadoEm: Long? = null,
    val ackServidor: String? = null
)

@Entity(tableName = "dead_letter_events")
data class DeadLetterEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val jornadaId: Long?,
    val tipoEvento: String,
    val payloadJson: String,
    val motivoFalha: String?,
    val stacktrace: String?,
    val tentativas: Int,
    val horario: Long = System.currentTimeMillis(),
    val vehicleId: String
)

@Entity(
    tableName = "vinculo_frota_wialon",
    indices = [
        Index(value = ["codigoFrotaLocal"], unique = true),
        Index(value = ["wialonUnitId"], unique = true)
    ]
)
data class VinculoFrotaWialonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val codigoFrotaLocal: String,
    val placa: String,
    val tipoVeiculo: String,
    val wialonUnitId: Long,
    val wialonNome: String,
    val operadorResponsavel: String,
    val criadoEm: Long = System.currentTimeMillis(),
    val atualizadoEm: Long = System.currentTimeMillis(),
    val ativo: Boolean = true,
    val ultimoKmWialon: Double = 0.0, // Novo campo para persistir o KM do site
    val wialonUniqueId: String? = null // IMEI ou ID Único para Wialon IPS
)

@Entity(tableName = "telemetria_history")
data class TelemetriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val lat: Double,
    val lng: Double,
    val speed: Float,
    val heading: Float,
    val km: Int,
    val altitude: Double = 0.0,
    val satellites: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "geofences")
data class GeofenceEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val type: Int, // 1: Circular, 2: Polígono
    val radius: Double = 0.0, // Para circular
    val pointsJson: String, // Lista de pontos JSON para polígonos
    val color: Int = 0,
    val maxSpeed: Float = 0f, // Alerta de velocidade local
    val active: Boolean = true
)

@Entity(tableName = "TabelaParadas")
data class ParadaEntity(
    @PrimaryKey val uuid: String = java.util.UUID.randomUUID().toString(),
    val tipo: String,
    val inicio: Long,
    val fim: Long? = null,
    val duracao: Long = 0, // em segundos
    val operador: String,
    val lat: Double,
    val lon: Double,
    val km: Int,
    val horimetro: Double,
    val syncStatus: String = "PENDENTE"
)
