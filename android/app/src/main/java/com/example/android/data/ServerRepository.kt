package com.example.android.data

import com.example.android.domain.model.InviteToken
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

    /** POST /api/auth/login — вход в существующий аккаунт. */
    suspend fun login(
        serverAddress: String,
        inviteToken: String,
        username: String,
        password: String,
    ): ApiResult<AuthResponse> {
        return connectionManager.getClient(serverAddress).login(inviteToken, username, password)
    }

    // ── Invite Tokens (Admin) ─────────────────────────────────────────

    /** POST /api/invite-tokens — создать токен приглашения. */
    suspend fun createInviteToken(
        serverAddress: String,
        label: String,
        maxUses: Int = 0,
        grantedRole: String = "MEMBER",
        requireApproval: Boolean = false,
    ): ApiResult<InviteToken> {
        return connectionManager.getClient(serverAddress)
            .createInviteToken(label, maxUses, grantedRole, requireApproval)
    }

    /** GET /api/invite-tokens — список всех токенов. */
    suspend fun getInviteTokens(serverAddress: String): ApiResult<List<InviteToken>> {
        return connectionManager.getClient(serverAddress).getInviteTokens()
    }

    /** DELETE /api/invite-tokens/{id} — отозвать токен. */
    suspend fun revokeInviteToken(serverAddress: String, tokenId: String): ApiResult<Unit> {
        return connectionManager.getClient(serverAddress).revokeInviteToken(tokenId)
    }

    fun disconnect(serverAddress: String) {
        connectionManager.removeClient(serverAddress)
    }
}
