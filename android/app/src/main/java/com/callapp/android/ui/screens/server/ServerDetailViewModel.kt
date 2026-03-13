package com.callapp.android.ui.screens.server

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserRole
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServerDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val repo = ServiceLocator.serverRepository

    private val _server = MutableStateFlow(
        repo.getServerById(serverId)
            ?: Server(serverId, "Server $serverId", "@unknown"),
    )
    val server: StateFlow<Server> = _server.asStateFlow()

    private val _membersState = MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val membersState: StateFlow<UiState<List<User>>> = _membersState.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val pendingRequests: StateFlow<List<JoinRequest>> = _pendingRequests.asStateFlow()

    init {
        loadMembers()
    }

    fun loadMembers() {
        viewModelScope.launch {
            _membersState.value = UiState.Loading
            val address = _server.value.address
            if (address.isBlank()) {
                _membersState.value = UiState.Error("Адрес сервера не указан")
                return@launch
            }
            when (val result = repo.getUsers(address)) {
                is ApiResult.Success -> {
                    _membersState.value = UiState.Success(result.data)

                    val currentUserId = ServiceLocator.currentUserId
                    _isAdmin.value = if (currentUserId.isNotEmpty()) {
                        result.data.any { it.id == currentUserId && it.role == UserRole.ADMIN }
                    } else {
                        false
                    }

                    if (_isAdmin.value) {
                        loadPendingRequests(address)
                    }
                }

                is ApiResult.Failure -> {
                    val message = when (val error = result.error) {
                        ApiError.NetworkError -> "Нет соединения с сервером"
                        ApiError.NotFound -> "Сервер не найден"
                        is ApiError.Unauthorized -> "Сессия истекла"
                        is ApiError.ValidationError -> error.message
                        is ApiError.UsernameTaken -> error.message
                        is ApiError.LoginLocked -> error.message
                        is ApiError.Forbidden -> error.message
                        is ApiError.DeprecatedEndpoint -> error.message
                        is ApiError.ServerError -> error.message ?: "Ошибка сервера"
                    }
                    _membersState.value = UiState.Error(message)
                }
            }
        }
    }

    private suspend fun loadPendingRequests(serverAddress: String) {
        val client = ServiceLocator.connectionManager.getClient(serverAddress)
        when (val result = client.getJoinRequests()) {
            is ApiResult.Success -> {
                _pendingRequests.value = result.data
            }

            is ApiResult.Failure -> {
                // Silently fail: pending requests are non-critical.
            }
        }
    }
}
