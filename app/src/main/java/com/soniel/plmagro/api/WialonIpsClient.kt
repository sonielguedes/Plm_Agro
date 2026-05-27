package com.soniel.plmagro.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * WialonIpsClient - Implementação Industrial via Socket TCP Bruto.
 * 
 * Esta classe remove qualquer dependência de HTTP/Retrofit para a camada de telemetria IPS,
 * operando exclusivamente via java.net.Socket para garantir conformidade com o protocolo
 * e eliminar erros HTTP 400 Bad Request.
 */
class WialonIpsClient {
    private val TAG = "IPS"
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val mutex = Mutex()

    /**
     * Valida o login no gateway IPS via handshake TCP.
     */
    suspend fun validateLogin(host: String, port: Int, uniqueId: String): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                ensureConnected(host, port, uniqueId)
                Result.success("#AL#1")
            } catch (e: Exception) {
                Log.e(TAG, "IPS_LOGIN_ERROR: ${e.message}")
                closeConnection()
                Result.failure(e)
            }
        }
    }

    /**
     * Garante que a conexão TCP está ativa e autenticada.
     */
    private fun ensureConnected(host: String, port: Int, imei: String) {
        if (socket == null || socket?.isClosed == true || socket?.isConnected == false) {
            closeConnection()
            
            Log.i(TAG, "SOCKET_CONNECTED: $host:$port")
            val newSocket = Socket()
            try {
                // Timeout de conexão: 15s
                newSocket.connect(InetSocketAddress(host, port), 15000)
                // Timeout de leitura: 10s
                newSocket.soTimeout = 10000
                
                val out = newSocket.getOutputStream()
                val inp = newSocket.getInputStream()

                // Protocolo IPS: Handshake de Login
                val loginPackage = WialonIpsProtocol.formatLoginPackage(imei)
                Log.i(TAG, "IPS_LOGIN_SENT: $imei")
                out.write(loginPackage.toByteArray())
                out.flush()

                // Aguarda ACK de Login (#AL#1)
                val buffer = ByteArray(128)
                val bytesRead = inp.read(buffer)
                if (bytesRead == -1) throw Exception("Conexão encerrada pelo servidor durante login")
                
                val response = String(buffer, 0, bytesRead).trim()
                if (!response.contains("#AL#1")) {
                    Log.e(TAG, "IPS_LOGIN_ERROR: Login rejeitado pelo servidor: $response")
                    throw Exception("Login rejeitado pelo servidor: $response")
                }

                Log.i(TAG, "IPS_LOGIN_ACK: $response")
                
                this.socket = newSocket
                this.outputStream = out
                this.inputStream = inp
            } catch (e: Exception) {
                newSocket.close()
                throw e
            }
        }
    }

    /**
     * Fecha a conexão TCP e limpa os streams.
     */
    private fun closeConnection() {
        try {
            if (socket != null) {
                Log.w(TAG, "SOCKET_DISCONNECTED")
            }
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            // Silencioso
        } finally {
            socket = null
            outputStream = null
            inputStream = null
        }
    }

    /**
     * Envia pacotes de telemetria/heartbeat via Socket TCP.
     */
    suspend fun sendMessages(host: String, port: Int, imei: String, messages: List<String>): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            var lastAck = ""
            try {
                ensureConnected(host, port, imei)
                val out = outputStream ?: throw Exception("OutputStream indisponível")
                val inp = inputStream ?: throw Exception("InputStream indisponível")
                val buffer = ByteArray(128)

                messages.forEach { msg ->
                    val start = System.currentTimeMillis()
                    Log.i(TAG, "IPS_PACKET_SENT: ${msg.trim()}")
                    
                    out.write(msg.toByteArray())
                    out.flush()
                    
                    // Aguarda ACK de Dados (#AD#1)
                    val bytesRead = inp.read(buffer)
                    if (bytesRead > 0) {
                        val resp = String(buffer, 0, bytesRead).trim()
                        val latency = System.currentTimeMillis() - start
                        
                        if (resp.contains("#AD#1")) {
                            Log.i(TAG, "IPS_PACKET_ACK: $resp (${latency}ms)")
                            lastAck = resp
                        } else if (resp.contains("#AD#-1")) {
                            Log.e(TAG, "IPS_PACKET_ERROR: Erro de dados (#AD#-1)")
                            throw Exception("Protocolo: Dados Rejeitados (#AD#-1)")
                        } else {
                            Log.w(TAG, "IPS_PACKET_ERROR: Resposta inesperada: $resp")
                            throw Exception("Protocolo: Resposta inesperada: $resp")
                        }
                    } else {
                        Log.e(TAG, "IPS_PACKET_ERROR: Timeout aguardando ACK")
                        throw Exception("Timeout: Sem ACK do servidor")
                    }
                }
                Result.success(lastAck)
            } catch (e: Exception) {
                Log.e(TAG, "IPS_TCP_ERROR: ${e.message}")
                closeConnection() // Força reconexão na próxima tentativa
                Result.failure(e)
            }
        }
    }
}
