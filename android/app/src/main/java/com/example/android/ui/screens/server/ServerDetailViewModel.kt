package com.example.android.ui.screens.server

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.data.MockJoinRequestRepository
import com.example.android.data.ServiceLocator
import com.example.android.domain.model.JoinRequest
import com.example.android.domain.model.Server
import com.example.android.domain.model.User
import com.example.android.network.result.ApiError
import com.example.android.network.result.ApiResult
import com.example.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServerDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val repo = ServiceLocator.serverRepository
    private val joinRequestRepo = MockJoinRequestRepository()

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
                    // TODO: определять isAdmin из ответа сервера
                    _isAdmin.value = result.data.any {
                        it.role == com.example.android.domain.model.UserRole.ADMIN
                    }
                    if (_isAdmin.value) {
                        _pendingRequests.value =
                            joinRequestRepo.getRequestsByServerId(serverId)
                    }
                }
                is ApiResult.Failure -> {
                    val message = when (result.error) {
                        ApiError.NetworkError -> "Нет соединения с сервером"
                        ApiError.Unauthorized -> "Сессия истекла"
                        ApiError.ServerError -> "Ошибка сервера"
                    }
                    _membersState.value = UiState.Error(message)
                }
            }
        }
    }
}
