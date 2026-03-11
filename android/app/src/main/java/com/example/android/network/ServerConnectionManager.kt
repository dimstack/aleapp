package com.example.android.network

import com.example.android.network.signaling.SignalingClient

class ServerConnectionManager {

    private val clients = mutableMapOf<String, ApiClient>()
    private val signalingClients = mutableMapOf<String, SignalingClient>()

    fun getClient(serverAddress: String): ApiClient {
        return clients.getOrPut(serverAddress) { ApiClient(serverAddress) }
    }

    /** Возвращает существующий или создаёт новый SignalingClient (без автоподключения). */
    fun getSignaling(serverAddress: String): SignalingClient {
        return signalingClients.getOrPut(serverAddress) {
            val token = clients[serverAddress]?.sessionToken.orEmpty()
            SignalingClient(serverAddress, token)
        }
    }

    fun removeClient(serverAddress: String) {
        signalingClients.remove(serverAddress)?.disconnect()
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
