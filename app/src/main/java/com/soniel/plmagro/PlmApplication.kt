package com.soniel.plmagro

import android.app.Application
import com.soniel.plmagro.model.DiagnosticRepository
import com.soniel.plmagro.model.PlmDatabase
import com.soniel.plmagro.model.PlmRepository

import androidx.work.*
import com.soniel.plmagro.sync.OutboxSyncWorker
import java.util.concurrent.TimeUnit

import com.soniel.plmagro.api.WialonRepository
import com.soniel.plmagro.api.WialonSessionManager
import com.soniel.plmagro.core.outbox.OutboxManager
import com.soniel.plmagro.model.UserPreferencesManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PlmApplication : Application() {
    companion object {
        lateinit var instance: PlmApplication
            private set
    }

    @Inject lateinit var database: PlmDatabase
    @Inject lateinit var repository: PlmRepository
    @Inject lateinit var wialonRepository: WialonRepository
    @Inject lateinit var sessionManager: WialonSessionManager
    @Inject lateinit var userPreferencesManager: UserPreferencesManager
    @Inject lateinit var diagnosticRepository: DiagnosticRepository
    @Inject lateinit var outboxManager: OutboxManager
    @Inject lateinit var sensorWatchdog: com.soniel.plmagro.core.watchdog.SensorWatchdog

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupSyncWork()
        cleanupOldData()
        
        // Inicia o motor de sincronização industrial
        outboxManager.startSyncLoop()
    }

    private fun cleanupOldData() {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            database.plmDao().deleteOldTelemetry(thirtyDaysAgo)
            database.plmDao().deleteOldSyncedEvents(thirtyDaysAgo)
            database.plmDao().deleteOldEvents(thirtyDaysAgo)
        }
    }

    private fun setupSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<OutboxSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "IndustrialSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
