package com.callapp.android.data

import com.callapp.android.domain.model.InviteToken
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.network.ServerConnectionManager
import com.callapp.android.network.dto.AuthResponse
import com.callapp.android.network.result.ApiResult

class ServerRepository(private val connectionManager: ServerConnectionManager) {

    /**
     * Список серверов, к которым подключён пользователь.
     * Reads from persistent SessionStore.
     */
    fun getConnectedServers(): List<Server> {
        val store = getSessionStoreOrNull() ?: return emptyList()
        return store.getConnectedServers()
    }

    /** Получить сервер по ID из локального списка. */
    fun getServerById(id: String): Server? =
        getConnectedServers().find { it.id == id }

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

    // ── Server Management ───────────────────────────────────────────

    /** GET /api/server — информация о сервере. */
    suspend fun getServerRemote(serverAddress: String): ApiResult<Server> {
        return connectionManager.getClient(serverAddress).getServer()
    }

    /** PUT /api/server — обновить данные сервера (только админ). */
    suspend fun updateServer(
        serverAddress: String,
        name: String? = null,
        username: String? = null,
        description: String? = null,
        imageUrl: String? = null,
    ): ApiResult<Server> {
        return connectionManager.getClient(serverAddress)
            .updateServer(name, username, description, imageUrl)
    }

    /** DELETE /api/server — удалить сервер (только админ). */
    suspend fun deleteServer(serverAddress: String): ApiResult<Unit> {
        return connectionManager.getClient(serverAddress).deleteServer()
    }

    // ── User Profile ────────────────────────────────────────────────

    /** GET /api/users/{id} — профиль пользователя. */
    suspend fun getUser(serverAddress: String, userId: String): ApiResult<User> {
        return connectionManager.getClient(serverAddress).getUser(userId)
    }

    /** PUT /api/users/{id} — обновить профиль. */
    suspend fun updateUser(
        serverAddress: String,
        userId: String,
        name: String? = null,
        username: String? = null,
        avatarUrl: String? = null,
    ): ApiResult<User> {
        return connectionManager.getClient(serverAddress)
            .updateUser(userId, name, username, avatarUrl)
    }

    // ── Favorites ───────────────────────────────────────────────────

    /** GET /api/favorites — список избранных (сетевой запрос). */
    suspend fun getFavoritesRemote(serverAddress: String): ApiResult<List<User>> {
        return connectionManager.getClient(serverAddress).getFavorites()
    }

    /** POST /api/favorites/{userId} — добавить в избранные. */
    suspend fun addFavorite(serverAddress: String, userId: String): ApiResult<Unit> {
        return connectionManager.getClient(serverAddress).addFavorite(userId)
    }

    /** DELETE /api/favorites/{userId} — удалить из избранных. */
    suspend fun removeFavorite(serverAddress: String, userId: String): ApiResult<Unit> {
        return connectionManager.getClient(serverAddress).removeFavorite(userId)
    }

    // ── Disconnect ──────────────────────────────────────────────────

    fun disconnect(serverAddress: String) {
        connectionManager.removeClient(serverAddress)
    }

    private fun getSessionStoreOrNull(): SessionStore? {
        return try {
            ServiceLocator.sessionStore
        } catch (_: UninitializedPropertyAccessException) {
            null
        }
    }
}
