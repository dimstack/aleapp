package com.callapp.android.ui.screens.joinrequests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.JoinRequestStatus
import com.callapp.android.domain.model.Server
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.apiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JoinRequestsUiState(
    val requests: List<JoinRequest> = emptyList(),
    val serverName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

interface JoinRequestsDependencies {
    fun getServerById(serverId: String): Server?
    suspend fun getServer(serverAddress: String): ApiResult<Server>
    suspend fun getJoinRequests(serverAddress: String): ApiResult<List<JoinRequest>>
    suspend fun updateJoinRequest(serverAddress: String, requestId: String, status: String): ApiResult<JoinRequest>
}

object DefaultJoinRequestsDependencies : JoinRequestsDependencies {
    override fun getServerById(serverId: String): Server? =
        ServiceLocator.serverRepository.getServerById(serverId)

    override suspend fun getServer(serverAddress: String): ApiResult<Server> =
        ServiceLocator.connectionManager.getClient(serverAddress).getServer()

    override suspend fun getJoinRequests(serverAddress: String): ApiResult<List<JoinRequest>> =
        ServiceLocator.connectionManager.getClient(serverAddress).getJoinRequests()

    override suspend fun updateJoinRequest(
        serverAddress: String,
        requestId: String,
        status: String,
    ): ApiResult<JoinRequest> =
        ServiceLocator.connectionManager.getClient(serverAddress).updateJoinRequest(requestId, status)
}

class JoinRequestsViewModel(
    savedStateHandle: SavedStateHandle,
    private val dependencies: JoinRequestsDependencies = DefaultJoinRequestsDependencies,
) : ViewModel() {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle = savedStateHandle,
        dependencies = DefaultJoinRequestsDependencies,
    )

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val serverAddress: String = dependencies.getServerById(serverId)?.address.orEmpty()

    private val _state = MutableStateFlow(JoinRequestsUiState())
    val state: StateFlow<JoinRequestsUiState> = _state.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            if (serverAddress.isBlank()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Сервер не найден",
                )
                return@launch
            }

            when (val serverResult = dependencies.getServer(serverAddress)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(serverName = serverResult.data.name)
                }

                is ApiResult.Failure -> Unit
            }

            when (val result = dependencies.getJoinRequests(serverAddress)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        requests = result.data.filter { it.status == JoinRequestStatus.PENDING },
                        isLoading = false,
                    )
                }

                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось загрузить заявки",
                            notFound = "Сервер не найден",
                        ),
                    )
                }
            }
        }
    }

    fun approve(requestId: String) {
        viewModelScope.launch {
            if (serverAddress.isBlank()) return@launch
            when (val result = dependencies.updateJoinRequest(serverAddress, requestId, "APPROVED")) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        requests = _state.value.requests.filter { it.id != requestId },
                    )
                }

                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        error = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось обработать заявку",
                            notFound = "Заявка не найдена",
                        ),
                    )
                }
            }
        }
    }

    fun decline(requestId: String) {
        viewModelScope.launch {
            if (serverAddress.isBlank()) return@launch
            when (val result = dependencies.updateJoinRequest(serverAddress, requestId, "DECLINED")) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        requests = _state.value.requests.filter { it.id != requestId },
                    )
                }

                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        error = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось обработать заявку",
                            notFound = "Заявка не найдена",
                        ),
                    )
                }
            }
        }
    }
}
