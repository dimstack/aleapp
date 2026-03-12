package com.callapp.android.ui.screens.joinrequests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.JoinRequestStatus
import com.callapp.android.network.result.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class JoinRequestsUiState(
    val requests: List<JoinRequest> = emptyList(),
    val serverName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class JoinRequestsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""

    private val _state = MutableStateFlow(JoinRequestsUiState())
    val state: StateFlow<JoinRequestsUiState> = _state

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val client = ServiceLocator.connectionManager
                .getClient(ServiceLocator.activeServerAddress)

            // Load server name
            when (val serverResult = client.getServer()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(serverName = serverResult.data.name)
                }
                is ApiResult.Failure -> { /* use fallback */ }
            }

            // Load join requests
            when (val result = client.getJoinRequests()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        requests = result.data.filter { it.status == JoinRequestStatus.PENDING },
                        isLoading = false,
                    )
                }
                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.error.name,
                    )
                }
            }
        }
    }

    fun approve(requestId: String) {
        viewModelScope.launch {
            val client = ServiceLocator.connectionManager
                .getClient(ServiceLocator.activeServerAddress)
            when (client.updateJoinRequest(requestId, "APPROVED")) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        requests = _state.value.requests.filter { it.id != requestId }
                    )
                }
                is ApiResult.Failure -> { /* silently fail */ }
            }
        }
    }

    fun decline(requestId: String) {
        viewModelScope.launch {
            val client = ServiceLocator.connectionManager
                .getClient(ServiceLocator.activeServerAddress)
            when (client.updateJoinRequest(requestId, "DECLINED")) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        requests = _state.value.requests.filter { it.id != requestId }
                    )
                }
                is ApiResult.Failure -> { /* silently fail */ }
            }
        }
    }
}
