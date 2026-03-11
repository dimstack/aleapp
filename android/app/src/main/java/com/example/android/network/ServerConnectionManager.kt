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
}
