package com.callapp.android.data

import com.callapp.android.domain.model.InviteToken
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.network.ServerConnectionManager
import com.callapp.android.network.dto.AuthResponse
import com.callapp.android.network.dto.ConnectResponse
import com.callapp.android.network.result.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ServerRepository(private val connectionManager: ServerConnectionManager) {

    fun getConnectedServers(): List<Server> {
        val store = getSessionStoreOrNull() ?: return emptyList()
        return store.getConnectedServers()
    }

    fun getServerById(id: String): Server? =
        getConnectedServers().find { it.id == id }

    fun observeConnectedServers(): Flow<List<Server>> {
        val store = getSessionStoreOrNull() ?: return flowOf(emptyList())
        return store.sessionsFlow.map { sessions ->
            sessions.values.map { session ->
                Server(
                    id = session.serverId.ifEmpty { session.serverAddress },
                    name = session.serverName.ifEmpty { session.serverAddress },
                    username = session.serverUsername,
                    address = session.serverAddress,
                )
            }
        }
    }

    fun observeServerById(id: String): Flow<Server?> =
        observeConnectedServers().map { servers -> servers.find { it.id == id } }

    suspend fun getUsers(serverAddress: String): ApiResult<List<User>> {
        return connectionManager.getClient(serverAddress).getUsers()
    }

    suspend fun connect(
        serverAddress: String,
        inviteToken: String,
    ): ApiResult<ConnectResponse> {
        return connectionManager.getClient(serverAddress).connect(inviteToken)
    }

    suspend fun login(
        serverAddress: String,
        inviteToken: String,
        username: String,
        password: String,
    ): ApiResult<AuthResponse> {
        return connectionManager.getClient(serverAddress).login(inviteToken, username, password)
    }

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

    suspend fun getInviteTokens(serverAddress: String): ApiResult<List<InviteToken>> {
        return connectionManager.getClient(serverAddress).getInviteTokens()
    }

    suspend fun revokeInviteToken(serverAddress: String, tokenId: String): ApiResult<Unit> {
        return connectionManager.getClient(serverAddress).revokeInviteToken(tokenId)
    }

    suspend fun getServerRemote(serverAddress: String): ApiResult<Server> {
        return connectionManager.getClient(serverAddress).getServer()
    }

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

    suspend fun getUser(serverAddress: String, userId: String): ApiResult<User> {
        return connectionManager.getClient(serverAddress).getUser(userId)
    }

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

    suspend fun getFavoritesRemote(serverAddress: String): ApiResult<List<User>> {
        return connectionManager.getClient(serverAddress).getFavorites()
    }

    suspend fun addFavorite(serverAddress: String, userId: String): ApiResult<Unit> {
        return connectionManager.getClient(serverAddress).addFavorite(userId)
    }

    suspend fun removeFavorite(serverAddress: String, userId: String): ApiResult<Unit> {
        return connectionManager.getClient(serverAddress).removeFavorite(userId)
    }

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
