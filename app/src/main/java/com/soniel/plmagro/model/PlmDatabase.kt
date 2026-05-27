package com.soniel.plmagro.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VehicleConfig::class,
        Operator::class,
        Journey::class,
        Event::class,
        OutboxEventEntity::class,
        DeadLetterEventEntity::class,
        VinculoFrotaWialonEntity::class,
        TelemetriaEntity::class,
        GeofenceEntity::class,
        ParadaEntity::class
    ],
    version = 20,
    exportSchema = false
)
@TypeConverters(PlmConverters::class)
abstract class PlmDatabase : RoomDatabase() {
    abstract fun plmDao(): PlmDao

    companion object {
        @Volatile
        private var INSTANCE: PlmDatabase? = null

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Criar TabelaParadas
                db.execSQL("CREATE TABLE IF NOT EXISTS `TabelaParadas` (`uuid` TEXT NOT NULL, `tipo` TEXT NOT NULL, `inicio` INTEGER NOT NULL, `fim` INTEGER, `duracao` INTEGER NOT NULL, `operador` TEXT NOT NULL, `lat` REAL NOT NULL, `lon` REAL NOT NULL, `km` INTEGER NOT NULL, `horimetro` REAL NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`uuid`))")
                
                // 2. Atualizar tabela journeys com novos campos industriais
                db.execSQL("ALTER TABLE `journeys` ADD COLUMN `lastHorimetro` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `journeys` ADD COLUMN `horimetroInicial` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `journeys` ADD COLUMN `timestamp_ultima_atualizacao` INTEGER")
                
                // 3. Atualizar tabela events com horímetro
                db.execSQL("ALTER TABLE `events` ADD COLUMN `horimetroAtTime` REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE sync_outbox SET syncStatus = 'PENDENTE' WHERE syncStatus = 'PENDING'")
                db.execSQL("UPDATE sync_outbox SET syncStatus = 'TENTANDO' WHERE syncStatus = 'RETRYING'")
                db.execSQL("UPDATE sync_outbox SET syncStatus = 'ENVIADO' WHERE syncStatus = 'SENT'")
                db.execSQL("UPDATE sync_outbox SET syncStatus = 'ERRO' WHERE syncStatus = 'ERROR'")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `telemetria_history` ADD COLUMN `altitude` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `telemetria_history` ADD COLUMN `satellites` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `geofences` (`id` INTEGER PRIMARY KEY NOT NULL, `name` TEXT NOT NULL, `type` INTEGER NOT NULL, `radius` REAL NOT NULL, `pointsJson` TEXT NOT NULL, `color` INTEGER NOT NULL, `maxSpeed` REAL NOT NULL, `active` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vinculo_frota_wialon` ADD COLUMN `wialonUniqueId` TEXT")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vehicle_config` ADD COLUMN `wialonUniqueId` TEXT")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vinculo_frota_wialon` ADD COLUMN `ultimoKmWialon` REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Adicionando tabela de vínculo que faltava
                db.execSQL("CREATE TABLE IF NOT EXISTS `vinculo_frota_wialon` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigoFrotaLocal` TEXT NOT NULL, `placa` TEXT NOT NULL, `tipoVeiculo` TEXT NOT NULL, `wialonUnitId` INTEGER NOT NULL, `wialonNome` TEXT NOT NULL, `operadorResponsavel` TEXT NOT NULL, `criadoEm` INTEGER NOT NULL, `atualizadoEm` INTEGER NOT NULL, `ativo` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_vinculo_frota_wialon_codigoFrotaLocal` ON `vinculo_frota_wialon` (`codigoFrotaLocal`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_vinculo_frota_wialon_wialonUnitId` ON `vinculo_frota_wialon` (`wialonUnitId`)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Adicionando tabela de telemetria
                db.execSQL("CREATE TABLE IF NOT EXISTS `telemetria_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `journeyId` INTEGER NOT NULL, `lat` REAL NOT NULL, `lng` REAL NOT NULL, `speed` REAL NOT NULL, `heading` REAL NOT NULL, `km` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Reestruturação da Outbox: Conversão segura de id (long) para eventId (UUID string)
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_outbox_new` (`eventId` TEXT NOT NULL, `jornadaId` INTEGER, `tipoEvento` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `retryCount` INTEGER NOT NULL, `lastAttempt` INTEGER, `errorMessage` TEXT, `timestamp` INTEGER NOT NULL, `vehicleId` TEXT NOT NULL, `operatorMatricula` TEXT, `prioridade` INTEGER NOT NULL, `origem` TEXT NOT NULL, `hashIntegridade` TEXT, `criadoEm` INTEGER NOT NULL, `enviadoEm` INTEGER, `ackServidor` TEXT, PRIMARY KEY(`eventId`))")
                
                // Mapeamento e preservação de dados
                try {
                    db.execSQL("INSERT INTO sync_outbox_new (eventId, jornadaId, tipoEvento, payloadJson, syncStatus, retryCount, lastAttempt, errorMessage, timestamp, vehicleId, operatorMatricula, prioridade, origem, hashIntegridade, criadoEm) " +
                            "SELECT CAST(id AS TEXT), jornadaId, type, payload, status, attempts, lastAttempt, errorMessage, createdAt, vehicleId, operatorMatricula, priority, origin, integrityHash, createdAt FROM sync_outbox")
                    db.execSQL("DROP TABLE IF EXISTS sync_outbox")
                } catch (e: Exception) {
                    // Se falhar (tabela não existe), apenas prossegue
                }
                db.execSQL("ALTER TABLE sync_outbox_new RENAME TO sync_outbox")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Adding Dead Letter Queue table
                db.execSQL("CREATE TABLE IF NOT EXISTS `dead_letter_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` TEXT NOT NULL, `jornadaId` INTEGER, `tipoEvento` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `motivoFalha` TEXT, `stacktrace` TEXT, `tentativas` INTEGER NOT NULL, `horario` INTEGER NOT NULL, `vehicleId` TEXT NOT NULL)")
            }
        }

        fun getDatabase(context: Context): PlmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlmDatabase::class.java,
                    "plm_agro_database"
                )
                .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class PlmConverters {
    @TypeConverter
    fun fromOperationalState(state: OperationalState): String = state.name

    @TypeConverter
    fun toOperationalState(state: String): OperationalState = OperationalState.valueOf(state)
}
