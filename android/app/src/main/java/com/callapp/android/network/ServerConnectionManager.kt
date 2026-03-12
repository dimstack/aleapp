package com.callapp.android.network

import com.callapp.android.network.signaling.SignalingClient
import java.util.concurrent.ConcurrentHashMap

class ServerConnectionManager {

    private val clients = ConcurrentHashMap<String, ApiClient>()
    private val signalingClients = ConcurrentHashMap<String, SignalingClient>()

    fun getClient(serverAddress: String): ApiClient {
        return clients.getOrPut(serverAddress) { ApiClient(serverAddress) }
    }

    /**
     * Возвращает существующий или создаёт новый SignalingClient (без автоподключения).
     * При каждом обращении обновляет токен из ApiClient, чтобы он не устаревал.
     */
    fun getSignaling(serverAddress: String): SignalingClient {
        val token = clients[serverAddress]?.sessionToken.orEmpty()
        return signalingClients.getOrPut(serverAddress) {
            SignalingClient(serverAddress, token)
        }.also { it.sessionToken = token }
    }

    fun removeClient(serverAddress: String) {
        signalingClients.remove(serverAddress)?.destroy()
        clients.remove(serverAddress)?.close()
    }

    fun restoreSession(serverAddress: String, sessionToken: String): ApiClient {
        val client = getClient(serverAddress)
        client.sessionToken = sessionToken
        return client
    }

    companion object {
        /**
         * Парсит invite-токен формата "server.example.com:3000/ABCD1234"
         * в пару (serverAddress, tokenCode).
         * Возвращает null если формат некорректный.
         */
        fun parseInviteToken(rawToken: String): Pair<String, String>? {
            val trimmed = rawToken.trim()
            val lastSlash = trimmed.lastIndexOf('/')
            if (lastSlash <= 0 || lastSlash == trimmed.lastIndex) return null
            val address = trimmed.substring(0, lastSlash)
            val code = trimmed.substring(lastSlash + 1)
            if (address.isBlank() || code.isBlank()) return null
            val baseUrl = if (address.startsWith("http")) address else "http://$address"
            return baseUrl to code
        }
    }
}
