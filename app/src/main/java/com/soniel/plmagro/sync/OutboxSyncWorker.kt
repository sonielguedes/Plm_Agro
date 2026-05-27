package com.soniel.plmagro.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soniel.plmagro.PlmApplication
import com.soniel.plmagro.core.outbox.SincronizadorOperacional

class OutboxSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PlmApplication
        val synchronizer = SincronizadorOperacional(app.database.plmDao(), app.wialonRepository)
        
        return try {
            synchronizer.sincronizarBatch()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
