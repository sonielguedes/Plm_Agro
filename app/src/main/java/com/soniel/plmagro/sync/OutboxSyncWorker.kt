package com.soniel.plmagro.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soniel.plmagro.api.WialonRepository
import com.soniel.plmagro.core.outbox.SincronizadorOperacional
import com.soniel.plmagro.model.PlmDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OutboxSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val plmDao: PlmDao,
    private val wialonRepository: WialonRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val synchronizer = SincronizadorOperacional(plmDao, wialonRepository)
        
        return try {
            synchronizer.sincronizarBatch()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
