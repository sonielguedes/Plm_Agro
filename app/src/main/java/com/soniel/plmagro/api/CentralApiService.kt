package com.soniel.plmagro.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SyncBatchRequest(
    val deviceId: String,
    val sessionId: String,
    val batchId: String,
    val events: List<Map<String, Any>>
)

data class SyncResult(
    val eventId: String,
    val status: String // ACKNOWLEDGED, DUPLICATED, INVALID_HASH, REJECTED
)

data class SyncBatchResponse(
    val batchId: String,
    val results: List<SyncResult>
)

interface CentralApiService {
    @POST("api/v1/sync/batch")
    suspend fun sendBatch(@Body request: SyncBatchRequest): Response<SyncBatchResponse>
}
