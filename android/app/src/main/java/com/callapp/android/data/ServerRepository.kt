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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ServerAvailabilityInfo(
    val status: ServerAvailabilityStatus,
    val message: String? = null,
)

sealed interface PendingApprovalEvent {
    data class Declined(
        val serverAddress: String,
        val serverName: String,
    ) : PendingApprovalEvent
}

private fun ApiError.invalidatesSession(): Boolean = when (this) {
    ApiError.NotFound -> true
    is ApiError.Unauthorized -> true
    is ApiError.Forbidden -> true
    else -> false
}

class ServerRepository(private val connectionManager: ServerConnectionManager) {

    private val availabilityMutex = Mutex()
    private val _availabilityByAddress = MutableStateFlow<Map<String, ServerAvailabilityInfo>>(emptyMap())
    val availabilityByAddress: StateFlow<Map<String, ServerAvailabilityInfo>> = _availabilityByAddress.asStateFlow()
    private val _pendingApprovalEvents = MutableSharedFlow<PendingApprovalEvent>(extraBufferCapacity = 1)
    val pendingApprovalEvents: SharedFlow<PendingApprovalEvent> = _pendingApprovalEvents.asSharedFlow()
    private val _favoriteUpdates = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val favoriteUpdates: SharedFlow<String> = _favoriteUpdates.asSharedFlow()
    private val _userUpdates = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userUpdates: SharedFlow<String> = _userUpdates.asSharedFlow()

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
                        description = session.serverDescription,
                        imageUrl = session.serverImageUrl,
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
        val result = connectionManager.getClient(serverAddress).connect(inviteToken)
        if (result is ApiResult.Success) {
            persistConnectSession(serverAddress, result.data)
        }
        return result
    }

    suspend fun login(
        serverAddress: String,
        inviteToken: String,
        username: String,
        password: String,
    ): ApiResult<AuthResponse> {
        val result = connectionManager.getClient(serverAddress).login(inviteToken, username, password)
        if (result is ApiResult.Success) {
            persistAuthSession(serverAddress, result.data)
        }
        return result
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
        coroutineScope {
            servers.mapNotNull { server ->
                val currentStatus = availabilityByAddress.value[server.address]?.status
                if (currentStatus != ServerAvailabilityStatus.CHECKING) {
                    async { refreshServerAvailability(server.address) }
                } else {
                    null
                }
            }.awaitAll()
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
                if (shouldInvalidateStoredSession(serverAddress, result.error)) {
                    invalidateServerSession(serverAddress)
                    return ServerAvailabilityInfo(
                        status = ServerAvailabilityStatus.UNAVAILABLE,
                        message = availabilityMessage(result.error),
                    )
                }
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
                            persistAuthSession(
                                serverAddress = pending.serverAddress,
                                response = response,
                                setActive = shouldSetActive,
                                fallbackServerName = pending.serverName,
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
                        else -> {
                            store.removePendingApproval(pending.serverAddress)
                            _pendingApprovalEvents.tryEmit(
                                PendingApprovalEvent.Declined(
                                    serverAddress = pending.serverAddress,
                                    serverName = pending.serverName,
                                ),
                            )
                        }
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
            .also { result ->
                if (result is ApiResult.Success) {
                    _userUpdates.tryEmit(serverAddress)
                }
            }
    }

    suspend fun uploadProfileImage(
        serverAddress: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): ApiResult<String> = connectionManager.getClient(serverAddress)
        .uploadProfileImage(bytes, fileName, mimeType)

    suspend fun uploadServerImage(
        serverAddress: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): ApiResult<String> = connectionManager.getClient(serverAddress)
        .uploadServerImage(bytes, fileName, mimeType)

    suspend fun getFavoritesRemote(serverAddress: String): ApiResult<List<User>> {
        return connectionManager.getClient(serverAddress).getFavorites()
    }

    suspend fun addFavorite(serverAddress: String, userId: String): ApiResult<Unit> {
        return connectionManager.getClient(serverAddress).addFavorite(userId).also { result ->
            if (result is ApiResult.Success) {
                _favoriteUpdates.tryEmit(serverAddress)
            }
        }
    }

    suspend fun removeFavorite(serverAddress: String, userId: String): ApiResult<Unit> {
        return connectionManager.getClient(serverAddress).removeFavorite(userId).also { result ->
            if (result is ApiResult.Success) {
                _favoriteUpdates.tryEmit(serverAddress)
            }
        }
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
                serverDescription = server.description,
                serverImageUrl = server.imageUrl,
            )
        }
    }

    private fun setAvailability(serverAddress: String, availability: ServerAvailabilityInfo) {
        _availabilityByAddress.value = _availabilityByAddress.value + (serverAddress to availability)
    }

    private fun trimAvailability(currentAddresses: Set<String>) {
        _availabilityByAddress.value = _availabilityByAddress.value.filterKeys { it in currentAddresses }
    }

    private fun invalidateServerSession(serverAddress: String) {
        connectionManager.removeClient(serverAddress)
        clearAvailability(serverAddress)

        val store = getSessionStoreOrNull()
        store?.removeSession(serverAddress)
        store?.removePendingApproval(serverAddress)

        if (ServiceLocator.activeServerAddress == serverAddress) {
            ServiceLocator.activeServerAddress = ""
            ServiceLocator.currentUserId = ""
            store?.activeServerAddress = ""
            store?.activeUserId = ""
        }
    }

    private fun shouldInvalidateStoredSession(serverAddress: String, error: ApiError): Boolean {
        if (error.invalidatesSession()) {
            return true
        }
        if (error is ApiError.NetworkError) {
            return false
        }
        val store = getSessionStoreOrNull() ?: return false
        val hasStoredSession = store.getSession(serverAddress) != null
        val clientHasToken = connectionManager.getClient(serverAddress).sessionToken != null
        return hasStoredSession && clientHasToken
    }

    private fun persistConnectSession(
        serverAddress: String,
        response: ConnectResponse,
    ) {
        if (!response.isJoined) return
        val store = getSessionStoreOrNull() ?: return
        connectionManager.restoreSession(serverAddress, response.sessionToken)
        ServiceLocator.activeServerAddress = serverAddress
        ServiceLocator.currentUserId = response.user?.id.orEmpty()
        store.saveSession(
            serverAddress = serverAddress,
            sessionToken = response.sessionToken,
            userId = response.user?.id.orEmpty(),
            serverName = response.server?.name.orEmpty(),
            serverUsername = response.server?.username.orEmpty(),
            serverId = response.server?.id.orEmpty(),
            serverDescription = response.server?.description.orEmpty(),
            serverImageUrl = response.server?.imageUrl,
        )
    }

    private fun persistAuthSession(
        serverAddress: String,
        response: AuthResponse,
        setActive: Boolean = true,
        fallbackServerName: String = "",
    ) {
        if (!response.isJoined) return
        val store = getSessionStoreOrNull() ?: return
        connectionManager.restoreSession(serverAddress, response.sessionToken)
        if (setActive) {
            ServiceLocator.activeServerAddress = serverAddress
            ServiceLocator.currentUserId = response.user?.id.orEmpty()
        }
        store.saveSession(
            serverAddress = serverAddress,
            sessionToken = response.sessionToken,
            userId = response.user?.id.orEmpty(),
            serverName = response.server?.name ?: fallbackServerName,
            serverUsername = response.server?.username.orEmpty(),
            serverId = response.server?.id.orEmpty(),
            serverDescription = response.server?.description.orEmpty(),
            serverImageUrl = response.server?.imageUrl,
            setActive = setActive,
        )
        store.removePendingApproval(serverAddress)
        setAvailability(serverAddress, ServerAvailabilityInfo(ServerAvailabilityStatus.AVAILABLE))
    }

    private fun availabilityMessage(error: ApiError): String = when (error) {
        ApiError.NetworkError -> "Сервер недоступен"
        ApiError.NotFound -> "Сервер недоступен"
        is ApiError.Unauthorized -> "Сервер недоступен"
        else -> "Сервер недоступен"
    }
}
