package com.callapp.android.data

import com.callapp.android.domain.model.InviteToken
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.domain.model.User
import com.callapp.android.network.ServerConnectionManager
import com.callapp.android.network.dto.AuthResponse
import com.callapp.android.network.dto.ConnectResponse
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ServerAvailabilityInfo(
    val status: ServerAvailabilityStatus,
    val message: String? = null,
)

class ServerRepository(private val connectionManager: ServerConnectionManager) {

    private val availabilityMutex = Mutex()
    private val _availabilityByAddress = MutableStateFlow<Map<String, ServerAvailabilityInfo>>(emptyMap())
    val availabilityByAddress: StateFlow<Map<String, ServerAvailabilityInfo>> = _availabilityByAddress.asStateFlow()

    fun getConnectedServers(): List<Server> {
        val store = getSessionStoreOrNull() ?: return emptyList()
        return store.getConnectedServers().map { server ->
            applyAvailability(server, availabilityByAddress.value)
        }
    }

    fun getServerById(id: String): Server? =
        getConnectedServers().find { it.id == id }

    fun observeConnectedServers(): Flow<List<Server>> {
        val store = getSessionStoreOrNull() ?: return flowOf(emptyList())
        return combine(store.sessionsFlow, availabilityByAddress) { sessions, availability ->
            sessions.values.map { session ->
                applyAvailability(
                    server = Server(
                        id = session.serverId.ifEmpty { session.serverAddress },
                        name = session.serverName.ifEmpty { session.serverAddress },
                        username = session.serverUsername,
                        address = session.serverAddress,
                    ),
                    availabilityByAddress = availability,
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

    suspend fun refreshConnectedServersAvailability() {
        val servers = getConnectedServers()
        trimAvailability(servers.map { it.address }.toSet())
        servers.forEach { server ->
            val currentStatus = availabilityByAddress.value[server.address]?.status
            if (currentStatus != ServerAvailabilityStatus.CHECKING) {
                refreshServerAvailability(server.address)
            }
        }
    }

    suspend fun refreshServerAvailability(serverAddress: String): ServerAvailabilityInfo {
        setAvailability(
            serverAddress = serverAddress,
            availability = ServerAvailabilityInfo(
                status = ServerAvailabilityStatus.CHECKING,
                message = "Проверка подключения...",
            ),
        )

        val availability = when (val result = getServerRemote(serverAddress)) {
            is ApiResult.Success -> {
                updateStoredServer(result.data)
                ServerAvailabilityInfo(ServerAvailabilityStatus.AVAILABLE)
            }

            is ApiResult.Failure -> {
                ServerAvailabilityInfo(
                    status = ServerAvailabilityStatus.UNAVAILABLE,
                    message = availabilityMessage(result.error),
                )
            }
        }

        setAvailability(serverAddress, availability)
        return availability
    }

    suspend fun processPendingApprovals() {
        val store = getSessionStoreOrNull() ?: return
        val pendingApprovals = store.getPendingApprovals().values.toList()
        if (pendingApprovals.isEmpty()) return

        pendingApprovals.forEach { pending ->
            when (
                val result = login(
                    serverAddress = pending.serverAddress,
                    inviteToken = pending.inviteToken,
                    username = pending.username,
                    password = pending.password,
                )
            ) {
                is ApiResult.Success -> {
                    val response = result.data
                    when {
                        response.isJoined -> {
                            val shouldSetActive = store.activeServerAddress.isBlank()
                            connectionManager.restoreSession(pending.serverAddress, response.sessionToken)
                            store.saveSession(
                                serverAddress = pending.serverAddress,
                                sessionToken = response.sessionToken,
                                userId = response.user?.id.orEmpty(),
                                serverName = response.server?.name ?: pending.serverName,
                                serverUsername = response.server?.username.orEmpty(),
                                serverId = response.server?.id.orEmpty(),
                                setActive = shouldSetActive,
                            )
                            if (shouldSetActive) {
                                ServiceLocator.activeServerAddress = pending.serverAddress
                                ServiceLocator.currentUserId = response.user?.id.orEmpty()
                            }
                            store.removePendingApproval(pending.serverAddress)
                        }

                        response.isPending -> {
                            connectionManager.restoreSession(pending.serverAddress, response.sessionToken)
                        }
                    }
                }

                is ApiResult.Failure -> {
                    when (result.error) {
                        ApiError.NetworkError -> Unit
                        else -> store.removePendingApproval(pending.serverAddress)
                    }
                }
            }
        }
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
        clearAvailability(serverAddress)
    }

    fun clearAvailability(serverAddress: String) {
        _availabilityByAddress.value = _availabilityByAddress.value - serverAddress
    }

    private fun getSessionStoreOrNull(): SessionStore? {
        return try {
            ServiceLocator.sessionStore
        } catch (_: UninitializedPropertyAccessException) {
            null
        }
    }

    private fun applyAvailability(
        server: Server,
        availabilityByAddress: Map<String, ServerAvailabilityInfo>,
    ): Server {
        val availability = availabilityByAddress[server.address]
        return if (availability == null) {
            server
        } else {
            server.copy(
                availabilityStatus = availability.status,
                availabilityMessage = availability.message,
            )
        }
    }

    private suspend fun updateStoredServer(server: Server) {
        val store = getSessionStoreOrNull() ?: return
        availabilityMutex.withLock {
            store.updateServerMetadata(
                serverAddress = server.address,
                serverId = server.id,
                serverName = server.name,
                serverUsername = server.username,
            )
        }
    }

    private fun setAvailability(serverAddress: String, availability: ServerAvailabilityInfo) {
        _availabilityByAddress.value = _availabilityByAddress.value + (serverAddress to availability)
    }

    private fun trimAvailability(currentAddresses: Set<String>) {
        _availabilityByAddress.value = _availabilityByAddress.value.filterKeys { it in currentAddresses }
    }

    private fun availabilityMessage(error: ApiError): String = when (error) {
        ApiError.NetworkError -> "Сервер недоступен"
        ApiError.NotFound -> "Сервер недоступен"
        is ApiError.Unauthorized -> "Сервер недоступен"
        else -> "Сервер недоступен"
    }
}
