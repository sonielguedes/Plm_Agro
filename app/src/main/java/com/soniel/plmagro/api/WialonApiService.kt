package com.soniel.plmagro.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WialonApiService {
    @GET("wialon/ajax.html")
    suspend fun login(
        @Query("svc") service: String = "token/login",
        @Query("params") params: String
    ): Response<WialonLoginResponse>

    @GET("wialon/ajax.html")
    suspend fun searchItems(
        @Query("svc") service: String = "core/search_items",
        @Query("params") params: String,
        @Query("sid") sessionId: String
    ): Response<WialonSearchResponse>

    @GET("wialon/ajax.html")
    suspend fun searchItem(
        @Query("svc") service: String = "core/search_item",
        @Query("params") params: String,
        @Query("sid") sessionId: String
    ): Response<WialonItemResponse>

    @GET("wialon/ajax.html")
    suspend fun registerCustomEvent(
        @Query("svc") service: String = "unit/register_custom_event",
        @Query("params") params: String,
        @Query("sid") sessionId: String
    ): Response<com.google.gson.JsonElement>

    @GET("wialon/ajax.html")
    suspend fun registerFueling(
        @Query("svc") service: String = "unit/register_fueling",
        @Query("params") params: String,
        @Query("sid") sessionId: String
    ): Response<com.google.gson.JsonElement>

    @GET("wialon/ajax.html")
    suspend fun getGeofences(
        @Query("svc") service: String = "resource/get_geofences_data",
        @Query("params") params: String,
        @Query("sid") sessionId: String
    ): Response<com.google.gson.JsonElement>

    @GET("wialon/ajax.html")
    suspend fun getDrivers(
        @Query("svc") service: String = "resource/get_drivers_data",
        @Query("params") params: String,
        @Query("sid") sessionId: String
    ): Response<com.google.gson.JsonElement>

    @GET("wialon/ajax.html")
    suspend fun checkCommands(
        @Query("svc") service: String = "unit/get_command_queue",
        @Query("params") params: String,
        @Query("sid") sessionId: String
    ): Response<com.google.gson.JsonElement>
}

data class WialonDriver(
    val id: Long,
    val n: String, // Name
    val c: String, // Code (Matrícula)
    val jp: Map<String, String>? = null // Custom properties
)

data class WialonGeofence(
    val id: Long,
    val n: String, // Name
    val t: Int,    // Type: 1-circle, 2-polygon, 3-polyline
    val r: Double, // Radius
    val c: Int,    // Color
    val b: WialonGeofenceBounds,
    val p: List<WialonGeofencePoint>? = null, // Points
    val s: Int? = null // Max speed
)

data class WialonGeofenceBounds(
    val min_x: Double,
    val min_y: Double,
    val max_x: Double,
    val max_y: Double
)

data class WialonGeofencePoint(
    val x: Double, // Lng
    val y: Double  // Lat
)

data class WialonGenericResponse(
    val error: Int? = null
)

data class WialonLoginResponse(
    val eid: String? = null,
    val error: Int? = null
)

data class WialonSearchResponse(
    val items: List<WialonUnit>? = null,
    val error: Int? = null
)

data class WialonItemResponse(
    val item: WialonUnit? = null,
    val error: Int? = null
)

data class WialonPos(
    val x: Double, // Longitude
    val y: Double, // Latitude
    val c: Int,    // Course/Km counter
    val f: Int     // Flags (Ignition)
)

data class WialonSensor(
    val n: String, // Name
    val t: String, // Type
    val v: Double? = null // Value
)

data class WialonUnit(
    val id: Long,
    val nm: String, // Name
    val cls: Int,   // Class
    val uid: String? = null, // Unique ID (IMEI)
    val prp: Map<String, String>? = null, // Custom properties
    val pos: WialonPos? = null,
    val sens: Map<String, WialonSensor>? = null
)
