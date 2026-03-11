package com.example.android.data

import com.example.android.domain.model.Server
import com.example.android.domain.model.User
import com.example.android.network.ServerConnectionManager
import com.example.android.network.dto.AuthResponse
import com.example.android.network.result.ApiResult

class ServerRepository(private val connectionManager: ServerConnectionManager) {

    /** Список серверов, к которым подключён пользователь (локальные данные). */
    fun getConnectedServers(): List<Server> = SampleData.servers

    /** Получить сервер по ID из локального списка. */
    fun getServerById(id: String): Server? = SampleData.servers.find { it.id == id }

    /** Избранные контакты (локальные данные). */
    fun getFavorites(): List<User> = SampleData.favorites

    /** GET /api/users — список участников сервера (сетевой запрос). */
    suspend fun getUsers(serverAddress: String): ApiResult<List<User>> {
        return connectionManager.getClient(serverAddress).getUsers()
    }

    /** POST /api/auth — авторизация по invite-токену. */
    suspend fun auth(
        serverAddress: String,
        inviteToken: String,
        displayName: String,
    ): ApiResult<AuthResponse> {
        return connectionManager.getClient(serverAddress).auth(inviteToken, displayName)
    }

    fun disconnect(serverAddress: String) {
        connectionManager.removeClient(serverAddress)
    }
}
