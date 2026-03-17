package com.callapp.android.ui.screens.servermanage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Server
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.apiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ServerManagementUiState(
    val isLoading: Boolean = true,
    val data: ServerManageData? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val actionError: String? = null,
)

interface ServerManagementDependencies {
    fun getServerById(serverId: String): Server?
    suspend fun updateServer(
        serverAddress: String,
        name: String,
        username: String,
        description: String?,
        imageUrl: String,
    ): ApiResult<Server>

    fun updateServerMetadata(
        serverAddress: String,
        serverId: String,
        serverName: String,
        serverUsername: String,
        serverDescription: String,
        serverImageUrl: String?,
    )
}

object DefaultServerManagementDependencies : ServerManagementDependencies {
    private val repository get() = ServiceLocator.serverRepository

    override fun getServerById(serverId: String): Server? = repository.getServerById(serverId)

    override suspend fun updateServer(
        serverAddress: String,
        name: String,
        username: String,
        description: String?,
        imageUrl: String,
    ): ApiResult<Server> = repository.updateServer(serverAddress, name, username, description, imageUrl)

    override fun updateServerMetadata(
        serverAddress: String,
        serverId: String,
        serverName: String,
        serverUsername: String,
        serverDescription: String,
        serverImageUrl: String?,
    ) {
        try {
            ServiceLocator.sessionStore.updateServerMetadata(
                serverAddress = serverAddress,
                serverId = serverId,
                serverName = serverName,
                serverUsername = serverUsername,
                serverDescription = serverDescription,
                serverImageUrl = serverImageUrl,
            )
        } catch (_: UninitializedPropertyAccessException) {
            // Ignore in previews/tests.
        }
    }
}

class ServerManagementViewModel(
    savedStateHandle: SavedStateHandle,
    private val dependencies: ServerManagementDependencies = DefaultServerManagementDependencies,
) : ViewModel() {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle = savedStateHandle,
        dependencies = DefaultServerManagementDependencies,
    )

    private val serverId: String = savedStateHandle["serverId"] ?: ""

    private val _state = MutableStateFlow(ServerManagementUiState())
    val state: StateFlow<ServerManagementUiState> = _state.asStateFlow()

    private var serverAddress: String = ""

    init {
        loadServer()
    }

    fun loadServer() {
        val server = dependencies.getServerById(serverId)
        if (server != null) {
            serverAddress = server.address
            _state.value = ServerManagementUiState(
                isLoading = false,
                data = ServerManageData(
                    id = server.id,
                    name = server.name,
                    username = server.username,
                    description = server.description,
                    imageUrl = server.imageUrl ?: "",
                ),
            )
        } else {
            _state.value = ServerManagementUiState(isLoading = false, error = "Сервер не найден")
        }
    }

    fun save(name: String, username: String, description: String?, imageUrl: String) {
        if (serverAddress.isEmpty()) return
        _state.value = _state.value.copy(isSaving = true, actionError = null)
        viewModelScope.launch {
            when (val result = dependencies.updateServer(serverAddress, name, username, description, imageUrl)) {
                is ApiResult.Success -> {
                    dependencies.updateServerMetadata(
                        serverAddress = serverAddress,
                        serverId = result.data.id,
                        serverName = result.data.name,
                        serverUsername = result.data.username,
                        serverDescription = result.data.description,
                        serverImageUrl = result.data.imageUrl,
                    )
                    _state.value = _state.value.copy(
                        isSaving = false,
                        saveSuccess = true,
                        data = _state.value.data?.copy(
                            name = result.data.name,
                            username = result.data.username,
                            description = result.data.description,
                            imageUrl = result.data.imageUrl.orEmpty(),
                        ),
                    )
                }

                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        actionError = apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось сохранить изменения",
                            notFound = "Сервер не найден",
                        ),
                    )
                }
            }
        }
    }

    fun clearActionError() {
        _state.value = _state.value.copy(actionError = null)
    }
}
