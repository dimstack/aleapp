package com.callapp.android.ui.screens.server

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserRole
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.UiState
import com.callapp.android.ui.common.apiErrorMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

sealed interface ServerDetailEvent {
    data class NavigateToCall(
        val serverAddress: String,
        val userId: String,
        val contactName: String,
    ) : ServerDetailEvent

    data object ServerDisconnected : ServerDetailEvent
}

interface ServerDetailDependencies {
    fun getServerById(serverId: String): Server?
    fun observeServerById(serverId: String): Flow<Server?>
    suspend fun getUsers(serverAddress: String): ApiResult<List<User>>
    suspend fun getJoinRequests(serverAddress: String): ApiResult<List<JoinRequest>>
    suspend fun removeUser(serverAddress: String, userId: String): ApiResult<Unit>
    fun disconnectServer(serverAddress: String)
    fun currentUserId(): String
}

object DefaultServerDetailDependencies : ServerDetailDependencies {
    private val repo get() = ServiceLocator.serverRepository

    override fun getServerById(serverId: String): Server? = repo.getServerById(serverId)

    override fun observeServerById(serverId: String): Flow<Server?> = repo.observeServerById(serverId)

    override suspend fun getUsers(serverAddress: String): ApiResult<List<User>> = repo.getUsers(serverAddress)

    override suspend fun getJoinRequests(serverAddress: String): ApiResult<List<JoinRequest>> =
        ServiceLocator.connectionManager.getClient(serverAddress).getJoinRequests()

    override suspend fun removeUser(serverAddress: String, userId: String): ApiResult<Unit> =
        ServiceLocator.connectionManager.getClient(serverAddress).deleteUser(userId)

    override fun disconnectServer(serverAddress: String) {
        ServiceLocator.clearServerSession(serverAddress)
    }

    override fun currentUserId(): String = ServiceLocator.currentUserId
}

class ServerDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val dependencies: ServerDetailDependencies = DefaultServerDetailDependencies,
) : ViewModel() {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle = savedStateHandle,
        dependencies = DefaultServerDetailDependencies,
    )

    private val serverId: String = savedStateHandle["serverId"] ?: ""

    private val _server = MutableStateFlow(
        dependencies.getServerById(serverId)
            ?: Server(serverId, "Server $serverId", "@unknown"),
    )
    val server: StateFlow<Server> = _server.asStateFlow()

    private val _membersState = MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val membersState: StateFlow<UiState<List<User>>> = _membersState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredMembers = MutableStateFlow<List<User>>(emptyList())
    val filteredMembers: StateFlow<List<User>> = _filteredMembers.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    val currentUserId: String get() = dependencies.currentUserId()

    private val _pendingRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val pendingRequests: StateFlow<List<JoinRequest>> = _pendingRequests.asStateFlow()

    private val _events = MutableSharedFlow<ServerDetailEvent>()
    val events: SharedFlow<ServerDetailEvent> = _events.asSharedFlow()

    init {
        observeServer()
        observeFilteredMembers()
        loadUsers()
    }

    private fun observeServer() {
        viewModelScope.launch {
            dependencies.observeServerById(serverId)
                .filterNotNull()
                .collect { updatedServer ->
                    _server.value = updatedServer
                }
        }
    }

    private fun observeFilteredMembers() {
        viewModelScope.launch {
            combine(_membersState, _searchQuery) { state, query ->
                filterMembers(state, query)
            }.collect { filtered ->
                _filteredMembers.value = filtered
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _membersState.value = UiState.Loading
            _actionError.value = null
            val address = _server.value.address
            if (address.isBlank()) {
                _membersState.value = UiState.Error("Адрес сервера не указан")
                return@launch
            }
            when (val result = dependencies.getUsers(address)) {
                is ApiResult.Success -> {
                    _membersState.value = UiState.Success(result.data)

                    val currentUserId = dependencies.currentUserId()
                    _isAdmin.value = if (currentUserId.isNotEmpty()) {
                        result.data.any { it.id == currentUserId && it.role == UserRole.ADMIN }
                    } else {
                        false
                    }

                    if (_isAdmin.value) {
                        loadPendingRequests(address)
                    } else {
                        _pendingRequests.value = emptyList()
                    }
                }

                is ApiResult.Failure -> {
                    _membersState.value = UiState.Error(
                        apiErrorMessage(
                            error = result.error,
                            fallback = "Ошибка сервера",
                            notFound = "Сервер не найден",
                        ),
                    )
                }
            }
        }
    }

    fun loadMembers() {
        loadUsers()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun removeUser(userId: String) {
        viewModelScope.launch {
            _actionError.value = null
            if (userId == dependencies.currentUserId()) {
                _actionError.value = "Нельзя удалить себя"
                return@launch
            }

            val address = _server.value.address
            if (address.isBlank()) {
                _actionError.value = "Адрес сервера не указан"
                return@launch
            }

            when (dependencies.removeUser(address, userId)) {
                is ApiResult.Success -> {
                    val currentState = _membersState.value
                    if (currentState is UiState.Success) {
                        _membersState.value = UiState.Success(
                            currentState.data.filterNot { it.id == userId },
                        )
                    }
                }

                is ApiResult.Failure -> {
                    _actionError.value = "Не удалось удалить пользователя"
                }
            }
        }
    }

    fun callUser(userId: String, contactName: String) {
        viewModelScope.launch {
            _events.emit(
                ServerDetailEvent.NavigateToCall(
                    serverAddress = _server.value.address,
                    userId = userId,
                    contactName = contactName,
                ),
            )
        }
    }

    fun disconnectServer() {
        viewModelScope.launch {
            val address = _server.value.address
            if (address.isBlank()) {
                _actionError.value = "Адрес сервера не указан"
                return@launch
            }

            dependencies.disconnectServer(address)
            _events.emit(ServerDetailEvent.ServerDisconnected)
        }
    }

    private suspend fun loadPendingRequests(serverAddress: String) {
        when (val result = dependencies.getJoinRequests(serverAddress)) {
            is ApiResult.Success -> {
                _pendingRequests.value = result.data
            }

            is ApiResult.Failure -> Unit
        }
    }

    private fun filterMembers(state: UiState<List<User>>, query: String): List<User> {
        val users = (state as? UiState.Success)?.data.orEmpty()
        if (query.isBlank()) {
            return users
        }
        val normalizedQuery = query.trim().lowercase()
        return users.filter { user ->
            user.name.lowercase().contains(normalizedQuery) ||
                user.username.lowercase().contains(normalizedQuery)
        }
    }
}
