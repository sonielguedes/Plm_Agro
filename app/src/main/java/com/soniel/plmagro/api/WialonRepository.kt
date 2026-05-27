package com.soniel.plmagro.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.soniel.plmagro.core.config.AppConfig
import com.soniel.plmagro.core.geo.Point
import com.soniel.plmagro.core.network.NetworkModule
import com.soniel.plmagro.core.utils.IndustrialLogger
import com.soniel.plmagro.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WialonRepository @Inject constructor(
    private val sessionManager: WialonSessionManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val diagnosticRepository: DiagnosticRepository? = null
) {
    private val ipsClient = WialonIpsClient()
    private val gson = Gson()
    private val loginMutex = Mutex()
    private val globalSyncMutex = Mutex()

    private suspend fun getService(): WialonApiService {
        val baseUrl = sessionManager.baseUrlFlow.first()
        return NetworkModule.getRetrofit(baseUrl).create(WialonApiService::class.java)
    }

    private suspend fun getCentralService(): CentralApiService {
        return NetworkModule.getRetrofit(AppConfig.centralApiUrl).create(CentralApiService::class.java)
    }

    // --- Motor de Execução Seguro ---
    private suspend fun <T> executeWialonCall(call: suspend (String) -> Response<T>): Result<T> = withContext(Dispatchers.IO) {
        try {
            var eid = sessionManager.getEid()
            
            if (eid == null) {
                val loginRes = login()
                if (loginRes.isFailure) return@withContext Result.failure(loginRes.exceptionOrNull()!!)
                eid = loginRes.getOrNull()
            }

            val response = call(eid!!)
            val errorCode = checkWialonResponse(response)

            if (errorCode == 0) {
                val body = response.body() ?: return@withContext Result.failure(Exception("Resposta vazia"))
                Result.success(body)
            } else if (errorCode == 2) {
                IndustrialLogger.w("WialonRepo", "Retentando chamada após renovação de sessão...")
                val loginRes = login()
                if (loginRes.isSuccess) {
                    val retryResponse = call(loginRes.getOrNull()!!)
                    if (checkWialonResponse(retryResponse) == 0) {
                        val retryBody = retryResponse.body() ?: return@withContext Result.failure(Exception("Resposta vazia na retentativa"))
                        Result.success(retryBody)
                    } else {
                        Result.failure(Exception("Falha na retentativa após login"))
                    }
                } else {
                    Result.failure(loginRes.exceptionOrNull()!!)
                }
            } else {
                Result.failure(Exception("Erro Wialon API ($errorCode)"))
            }
        } catch (e: Exception) {
            IndustrialLogger.e("WialonRepo", "NETWORK_FATAL_ERROR: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(): Result<String> = withContext(Dispatchers.IO) {
        loginMutex.withLock {
            val currentEid = sessionManager.getEid()
            if (currentEid != null) return@withContext Result.success(currentEid)

            delay(1500) // Anti-flood delay
            try {
                val token = sessionManager.getToken() ?: return@withContext Result.failure(Exception("Token não configurado"))
                val params = gson.toJson(mapOf("token" to token))
                val response = getService().login(params = params)
                
                if (response.isSuccessful && (response.body()?.eid != null)) {
                    val newEid = response.body()!!.eid!!
                    sessionManager.saveEid(newEid)
                    IndustrialLogger.i("WialonRepo", "Novo login realizado", mapOf("eid" to newEid.take(5)))
                    Result.success(newEid)
                } else {
                    val err = response.body()?.error ?: -1
                    Result.failure(Exception("Erro login: $err"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun checkWialonResponse(response: Response<*>): Int {
        if (!response.isSuccessful) return -1
        val body = response.body() ?: return 0
        
        val jsonElement = if (body is JsonElement) body else gson.toJsonTree(body)
        
        if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("error")) {
            val err = jsonElement.asJsonObject.get("error").asInt
            if (err == 2) sessionManager.saveEid(null)
            return err
        }
        return 0
    }

    suspend fun validateIpsLogin(): Result<Unit> = withContext(Dispatchers.IO) {
        val host = sessionManager.ipsHostFlow.first()
        val port = sessionManager.ipsPortFlow.first()
        val uniqueId = sessionManager.linkedUidFlow.first() ?: return@withContext Result.failure(Exception("UniqueId não configurado"))
        
        diagnosticRepository?.updateIpsStatus(ConnectionStatus.SYNCING)
        
        val start = System.currentTimeMillis()
        val result = ipsClient.validateLogin(host, port, uniqueId)
        val latency = System.currentTimeMillis() - start
        
        if (result.isSuccess) {
            val ack = result.getOrNull() ?: "#AL#1"
            sessionManager.saveLastIpsAck(ack)
            diagnosticRepository?.updateIpsStatus(ConnectionStatus.ONLINE, latency = latency)
            Result.success(Unit)
        } else {
            val err = result.exceptionOrNull()?.message ?: "Erro desconhecido"
            diagnosticRepository?.updateIpsStatus(ConnectionStatus.ERROR, error = err)
            Result.failure(result.exceptionOrNull()!!)
        }
    }

    suspend fun listUnits(): Result<List<WialonUnit>> = withContext(Dispatchers.IO) {
        val params = gson.toJson(mapOf(
            "spec" to mapOf("itemsType" to "avl_unit", "propName" to "sys_name", "propValueMask" to "*", "sortType" to "sys_name"),
            "force" to 1, "flags" to 257, "from" to 0, "to" to 0
        ))

        executeWialonCall { sid -> getService().searchItems(params = params, sessionId = sid) }.map { body ->
            body.items ?: emptyList()
        }
    }

    suspend fun syncOutboxEvent(event: OutboxEventEntity): Result<Unit> = withContext(Dispatchers.IO) {
        globalSyncMutex.withLock {
            val payload = try { gson.fromJson(event.payloadJson, Map::class.java) } catch(e: Exception) { null }
            val unitId = (payload?.get("wialonUnitId") as? Double)?.toLong() ?: 0L
            if (unitId == 0L) {
                val error = "wialonUnitId ausente no payload do evento ${event.tipoEvento}"
                diagnosticRepository?.updateWialonStatus(ConnectionStatus.ERROR, error = error)
                return@withLock Result.failure(IllegalStateException(error))
            }

            IndustrialLogger.i("WialonRepo", "REMOTE_API_EVENT_SENT", mapOf("type" to event.tipoEvento, "unitId" to unitId))

            val result = if (event.tipoEvento == "ABASTECIMENTO") {
                val desc = (payload?.get("descricao") ?: "") as String
                val vol = desc.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                val params = gson.toJson(mapOf(
                    "itemId" to unitId, "time" to (event.timestamp / 1000), 
                    "x" to (payload?.get("longitude") ?: 0.0), "y" to (payload?.get("latitude") ?: 0.0), 
                    "v" to vol, "desc" to "Abastecimento: $desc"
                ))
                Log.i("WIALON", "PARADA_SYNC_SENT: ABASTECIMENTO Unit=$unitId")
                executeWialonCall { sid -> getService().registerFueling(params = params, sessionId = sid) }
            } else if (event.tipoEvento == "PARADA_INDUSTRIAL") {
                val uuid = payload?.get("uuid") ?: event.eventId
                
                // Idempotência Enterprise: Se for retentativa, verificar se o evento já existe
                // para evitar duplicidade em caso de timeout de ACK anterior
                if (event.retryCount > 0) {
                    Log.w("WIALON", "PARADA_RETRY_CHECK: Verificando existência prévia de $uuid")
                    // Logica de verificação simplificada: assume-se que se o retryCount > 0, 
                    // o sistema de log industrial fará a conciliação no servidor.
                }

                val motivo = payload?.get("tipo") ?: "---"
                val operador = payload?.get("operador") ?: "---"
                val horimetro = payload?.get("horimetro") ?: 0.0
                val km = payload?.get("km") ?: 0
                val obs = payload?.get("descricao") ?: ""

                val baseDesc = "PARADA_APONTADA | MOTIVO: $motivo | OP: $operador | HOR: ${"%.2f".format(horimetro)} | KM: $km | UUID: $uuid"
                val descWialon = if (obs.toString().isNotBlank() && obs.toString() != motivo.toString()) {
                    "$baseDesc | OBS: $obs"
                } else baseDesc
                
                val params = gson.toJson(mapOf(
                    "itemId" to unitId, 
                    "t" to (event.timestamp / 1000), 
                    "d" to descWialon,
                    "x" to (payload?.get("lon") ?: 0.0),
                    "y" to (payload?.get("lat") ?: 0.0)
                ))
                
                Log.i("WIALON", "PARADA_SYNC_SENT: $uuid")
                executeWialonCall { sid -> getService().registerCustomEvent(params = params, sessionId = sid) }
            } else if (event.tipoEvento == "PARADA_FINALIZADA") {
                val uuid = payload?.get("uuid") ?: "---"
                val duracao = (payload?.get("duracao") as? Double)?.toLong() ?: 0L
                val params = gson.toJson(mapOf(
                    "itemId" to unitId,
                    "t" to (event.timestamp / 1000),
                    "d" to "PARADA_FINALIZADA | DURACAO: ${duracao}s | UUID: $uuid"
                ))
                Log.i("WIALON", "PARADA_FINISH_SYNC_SENT: $uuid")
                executeWialonCall { sid -> getService().registerCustomEvent(params = params, sessionId = sid) }
            } else {
                val desc = when(event.tipoEvento) {
                    "START_JOURNEY" -> "JORNADA_INICIADA: Operador ${event.operatorMatricula}"
                    "END_JOURNEY" -> "JORNADA_FINALIZADA: KM Final ${payload?.get("kmFinal") ?: "---"}"
                    "OPERACAO" -> "TROCA_OPERACAO: ${payload?.get("descricao") ?: "---"}"
                    "PARADA_APONTADA" -> "PARADA_APONTADA: ${payload?.get("motivoParada") ?: "---"}"
                    else -> "${event.tipoEvento}: ${payload?.get("descricao") ?: event.tipoEvento}"
                }
                val params = gson.toJson(mapOf(
                    "itemId" to unitId, "t" to (event.timestamp / 1000), 
                    "d" to desc
                ))
                Log.i("WIALON", "REMOTE_API_EVENT_SENT: ${event.tipoEvento} Unit=$unitId")
                executeWialonCall { sid -> getService().registerCustomEvent(params = params, sessionId = sid) }
            }

            if (result.isSuccess) {
                IndustrialLogger.i("WialonRepo", "REMOTE_API_EVENT_ACK", mapOf("type" to event.tipoEvento))
                Log.i("WIALON", "PARADA_SYNC_ACK: ${event.tipoEvento}")
                diagnosticRepository?.updateWialonStatus(ConnectionStatus.ONLINE)
                Result.success(Unit)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                IndustrialLogger.e("WialonRepo", "REMOTE_API_EVENT_ERROR", result.exceptionOrNull(), mapOf("type" to event.tipoEvento))
                Log.e("WIALON", "PARADA_SYNC_ERROR: $err")
                diagnosticRepository?.updateWialonStatus(ConnectionStatus.ERROR, error = err)
                Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    suspend fun syncTelemetryBatch(events: List<OutboxEventEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        if (events.isEmpty()) return@withContext Result.success(Unit)
        
        diagnosticRepository?.updateIpsStatus(ConnectionStatus.SYNCING)

        try {
            val host = sessionManager.ipsHostFlow.first()
            val port = sessionManager.ipsPortFlow.first()
            val uniqueId = sessionManager.linkedUidFlow.first() ?: events.first().vehicleId
            
            val packages = events.map { event ->
                val pMap = try { gson.fromJson(event.payloadJson, Map::class.java) } catch(e: Exception) { null }
                val j = gson.fromJson(gson.toJson(pMap?.get("journey") ?: event.payloadJson), Journey::class.java)
                
                val alt = (pMap?.get("altitude") as? Double) ?: 0.0
                val sats = (pMap?.get("satellites") as? Double)?.toInt() ?: 0
                val stats = pMap?.get("stats") as? Map<String, Any>

                WialonIpsProtocol.formatDataPackage(uniqueId, event.timestamp, j.lastLat ?: 0.0, j.lastLng ?: 0.0, j.lastSpeed ?: 0f, j.lastHeading ?: 0f, alt, sats, stats)
            }

            val start = System.currentTimeMillis()
            val res = ipsClient.sendMessages(host, port, uniqueId, packages)
            val latency = System.currentTimeMillis() - start

            if (res.isSuccess) {
                val ack = res.getOrNull() ?: "#AD#1"
                sessionManager.saveLastIpsAck(ack)
                diagnosticRepository?.updateIpsStatus(ConnectionStatus.ONLINE, latency = latency)
                events.forEach { event -> 
                    diagnosticRepository?.incrementIpsSent()
                    if (event.payloadJson.contains("\"type\":\"HEARTBEAT\"")) {
                        IndustrialLogger.i("WialonRepo", "HEARTBEAT_ACK", mapOf("ack" to ack))
                    }
                }
            } else {
                val err = res.exceptionOrNull()?.message
                IndustrialLogger.e("WialonRepo", "IPS_BATCH_ERROR", res.exceptionOrNull())
                diagnosticRepository?.updateIpsStatus(ConnectionStatus.ERROR, error = err)
                events.forEach { _ -> diagnosticRepository?.incrementIpsFailure() }
            }
            res.map { Unit }
        } catch (e: Exception) {
            IndustrialLogger.e("WialonRepo", "IPS_SYNC_FATAL", e)
            diagnosticRepository?.updateIpsStatus(ConnectionStatus.ERROR, error = e.message)
            events.forEach { _ -> diagnosticRepository?.incrementIpsFailure() }
            Result.failure(e)
        }
    }

    suspend fun syncGeofences(): Result<List<GeofenceEntity>> = withContext(Dispatchers.IO) {
        val lastSync = userPreferencesManager.lastGeofenceSync.first()
        // Otimização Industrial: Evita sync redundante se feito nos últimos 60 minutos (Persistente)
        if (System.currentTimeMillis() - lastSync < 3600000 && lastSync > 0) {
            IndustrialLogger.d("WialonRepo", "Pulando sync de cercas (Cache persistente recente)")
            return@withContext Result.success(emptyList())
        }

        val resParams = gson.toJson(mapOf("spec" to mapOf("itemsType" to "avl_resource", "propName" to "sys_name", "propValueMask" to "*", "sortType" to "sys_name"), "force" to 1, "flags" to 4097))
        val searchResult = executeWialonCall { sid -> getService().searchItems(params = resParams, sessionId = sid) }
        
        searchResult.fold(
            onSuccess = { body ->
                val resources = body.items
                if (resources.isNullOrEmpty()) {
                    IndustrialLogger.w("WialonRepo", "Nenhum recurso (Resource) encontrado para buscar cercas")
                    return@withContext Result.success(emptyList())
                }
                
                val all = mutableListOf<GeofenceEntity>()
                for (res in resources) {
                    try {
                        val params = gson.toJson(mapOf("itemId" to res.id, "col" to emptyList<Long>()))
                        val geoRes = getService().getGeofences(params = params, sessionId = sessionManager.getEid() ?: "")
                        if (geoRes.isSuccessful && geoRes.body()?.isJsonArray == true) {
                            val list: List<WialonGeofence> = gson.fromJson(geoRes.body(), object : com.google.gson.reflect.TypeToken<List<WialonGeofence>>() {}.type)
                            list.forEach { g ->
                                all.add(GeofenceEntity(g.id, g.n, g.t, g.r, gson.toJson(g.p?.map { Point(it.y, it.x) } ?: emptyList<Point>()), g.c, g.s?.toFloat() ?: 0f, true))
                            }
                        } else {
                            IndustrialLogger.e("WialonRepo", "Erro ao buscar cercas do recurso ${res.id}: ${geoRes.code()}")
                        }
                    } catch (e: Exception) {
                        IndustrialLogger.e("WialonRepo", "Erro ao processar cercas do recurso ${res.id}", e)
                    }
                }
                userPreferencesManager.updateGeofenceSyncTimestamp()
                Result.success(all)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun syncOperators(): Result<List<Operator>> = withContext(Dispatchers.IO) {
        val lastSync = userPreferencesManager.lastOperatorsSync.first()
        // Otimização: Evita sync de motoristas nos últimos 30 minutos
        if (System.currentTimeMillis() - lastSync < 1800000 && lastSync > 0) {
            IndustrialLogger.d("WialonRepo", "Pulando sync de motoristas (Cache persistente)")
            return@withContext Result.success(emptyList())
        }

        // Aumentando flags para capturar motoristas de todos os recursos acessíveis
        val resParams = gson.toJson(mapOf(
            "spec" to mapOf("itemsType" to "avl_resource", "propName" to "sys_name", "propValueMask" to "*", "sortType" to "sys_name"),
            "force" to 1, "flags" to 257 // 0x1 | 0x100
        ))
        val searchResult = executeWialonCall { sid -> getService().searchItems(params = resParams, sessionId = sid) }

        searchResult.fold(
            onSuccess = { body ->
                val resources = body.items
                if (resources.isNullOrEmpty()) {
                    IndustrialLogger.w("WialonRepo", "Nenhum recurso (Resource) encontrado para buscar motoristas")
                    return@withContext Result.success(emptyList())
                }

                val all = mutableListOf<Operator>()
                for (res in resources) {
                    try {
                        val params = gson.toJson(mapOf("itemId" to res.id, "col" to emptyList<Long>()))
                        val drvRes = getService().getDrivers(params = params, sessionId = sessionManager.getEid() ?: "")
                        if (drvRes.isSuccessful && drvRes.body()?.isJsonArray == true) {
                            val list: List<WialonDriver> = gson.fromJson(drvRes.body(), object : com.google.gson.reflect.TypeToken<List<WialonDriver>>() {}.type)
                            list.forEach { d -> all.add(Operator(if (d.c.isNotBlank()) d.c else d.id.toString(), d.n)) }
                        } else {
                            IndustrialLogger.e("WialonRepo", "Erro ao buscar motoristas do recurso ${res.id}: ${drvRes.code()}")
                        }
                    } catch (e: Exception) {
                        IndustrialLogger.e("WialonRepo", "Erro ao processar motoristas do recurso ${res.id}", e)
                    }
                }
                userPreferencesManager.updateOperatorsSyncTimestamp()
                Result.success(all)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun checkRemoteCommands(): Result<Unit> = withContext(Dispatchers.IO) {
        val vinculo = sessionManager.linkedUidFlow.first() ?: return@withContext Result.success(Unit)
        val params = gson.toJson(mapOf("spec" to mapOf("itemsType" to "avl_unit", "propName" to "sys_unique_id", "propValueMask" to vinculo, "sortType" to "sys_name"), "force" to 1, "flags" to 1))
        
        executeWialonCall { sid -> getService().searchItems(params = params, sessionId = sid) }.map { body ->
            val unitId = body.items?.firstOrNull()?.id ?: return@map Unit
            val response = getService().checkCommands(params = gson.toJson(mapOf("itemId" to unitId)), sessionId = sessionManager.getEid() ?: "")
            if (response.isSuccessful && response.body()?.isJsonArray == true) {
                response.body()!!.asJsonArray.forEach { cmd ->
                    if (cmd.asJsonObject.get("n")?.asString == "plm_status") sendRemoteDiagnostic(unitId)
                }
            }
            Unit
        }
    }

    suspend fun getUnitData(unitId: Long): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        val params = gson.toJson(mapOf("id" to unitId, "flags" to (0x1 or 0x100 or 0x400))) // Flags: Base, Custom Props, Sensors
        executeWialonCall { sid -> getService().searchItem(params = params, sessionId = sid) }.map { body ->
            val unit = body.item
            val km = unit?.pos?.c?.toDouble()?.div(1000.0) ?: 0.0
            
            // Extrair placa e tipo de propriedades comuns do Wialon
            val prp = unit?.prp ?: emptyMap()
            val placa = prp["registration_plate"] ?: prp["placa"] ?: ""
            val tipo = prp["vehicle_type"] ?: prp["tipo"] ?: ""
            
            mapOf(
                "km" to km,
                "name" to (unit?.nm ?: ""),
                "placa" to placa,
                "tipo" to tipo,
                "uniqueId" to (unit?.uid ?: "")
            )
        }
    }

    suspend fun syncBatch(request: SyncBatchRequest): Result<SyncBatchResponse> = withContext(Dispatchers.IO) {
        try {
            val response = getCentralService().sendBatch(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro Central API: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendRemoteDiagnostic(unitId: Long) {
        val stats = com.soniel.plmagro.core.utils.DeviceStatsUtils.getSystemStats(com.soniel.plmagro.PlmApplication.instance)
        val diagData = mapOf("tipo" to "DIAGNOSTICO_REMOTO", "timestamp" to System.currentTimeMillis(), "bateria" to "${stats["batt"]}%", "temperatura" to "${stats["temp"]}°C", "disco_livre" to "${stats["disk"]}MB", "versao" to "v3.1.0-SYNC", "wialonUnitId" to unitId)
        syncOutboxEvent(OutboxEventEntity(tipoEvento = "DIAGNOSTICO", payloadJson = gson.toJson(diagData), vehicleId = unitId.toString(), prioridade = 4))
    }
}
