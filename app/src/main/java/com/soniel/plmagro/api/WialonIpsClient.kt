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
 * WialonIpsClient - Implementação robusta via Socket TCP para Wialon IPS v2.2.
 */
class WialonIpsClient {
    private val TAG = "IPS"
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val mutex = Mutex()

    /**
     * Valida o login no gateway IPS.
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
            
            Log.i(TAG, "SOCKET_CONNECTING: $host:$port")
            val newSocket = Socket()
            try {
                // Timeouts industriais
                newSocket.connect(InetSocketAddress(host, port), 20000)
                newSocket.soTimeout = 15000
                
                val out = newSocket.getOutputStream()
                val inp = newSocket.getInputStream()

                // Protocolo IPS: Login
                val loginPackage = WialonIpsProtocol.formatLoginPackage(imei)
                Log.i(TAG, "IPS_LOGIN_SENT: $imei")
                out.write(loginPackage.toByteArray())
                out.flush()

                // Resposta de Login: #AL#1 (Sucesso) ou #AL#0 (Falha)
                val scanner = java.util.Scanner(inp).useDelimiter("\r\n")
                if (scanner.hasNext()) {
                    val response = scanner.next().trim()
                    Log.i(TAG, "IPS_LOGIN_ACK: $response")

                    if (response.contains("#AL#1")) {
                        this.socket = newSocket
                        this.outputStream = out
                        this.inputStream = inp
                    } else if (response.contains("#AL#0")) {
                        throw Exception("Login rejeitado (Erro 0): Verifique o UniqueID")
                    } else {
                        throw Exception("Resposta de login inesperada: $response")
                    }
                } else {
                    throw Exception("Sem resposta do servidor no handshake")
                }
            } catch (e: Exception) {
                newSocket.close()
                throw e
            }
        }
    }

    private fun closeConnection() {
        try {
            if (socket != null) Log.w(TAG, "SOCKET_CLOSED")
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
     * Envia pacotes de telemetria e trata os ACKs do protocolo IPS 2.2.
     */
    suspend fun sendMessages(host: String, port: Int, imei: String, messages: List<String>): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            var lastAck = ""
            try {
                ensureConnected(host, port, imei)
                val out = outputStream ?: throw Exception("Stream de saída indisponível")
                val inp = inputStream ?: throw Exception("Stream de entrada indisponível")
                
                // Usamos Scanner para ler o fluxo delimitado por \r\n de forma resiliente
                val scanner = java.util.Scanner(inp).useDelimiter("\r\n")

                messages.forEach { msg ->
                    val start = System.currentTimeMillis()
                    Log.i(TAG, "IPS_PACKET_SENT: ${msg.trim()}")
                    
                    out.write(msg.toByteArray())
                    out.flush()
                    
                    // Aguarda o próximo token (ACK) do servidor
                    if (scanner.hasNext()) {
                        val resp = scanner.next().trim()
                        val latency = System.currentTimeMillis() - start
                        
                        when {
                            resp.contains("#AD#1") -> {
                                Log.i(TAG, "IPS_PACKET_ACK: $resp (${latency}ms)")
                                lastAck = resp
                            }
                            resp.contains("#AD#0") -> {
                                Log.e(TAG, "IPS_PACKET_ERROR: Pacote rejeitado (#AD#0)")
                                throw Exception("Protocolo: Pacote Rejeitado (#AD#0)")
                            }
                            resp.contains("#AD#-1") -> {
                                Log.e(TAG, "IPS_PACKET_ERROR: Erro de estrutura (#AD#-1)")
                                throw Exception("Protocolo: Erro de Dados (#AD#-1)")
                            }
                            else -> {
                                Log.w(TAG, "IPS_PACKET_ERROR: Resposta inesperada: $resp")
                                // Se recebemos algo como #AD#13, registramos mas não abortamos se contiver #AD#1
                                if (resp.contains("#AD#1")) {
                                    lastAck = resp
                                } else {
                                    throw Exception("Protocolo: Resposta desconhecida: $resp")
                                }
                            }
                        }
                    } else {
                        throw Exception("Conexão interrompida (Scanner sem dados)")
                    }
                }
                Result.success(lastAck)
            } catch (e: Exception) {
                Log.e(TAG, "IPS_TCP_FATAL: ${e.message}")
                closeConnection()
                Result.failure(e)
            }
        }
    }
}
