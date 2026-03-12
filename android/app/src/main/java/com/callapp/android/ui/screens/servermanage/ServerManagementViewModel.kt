package com.callapp.android.ui.screens.servermanage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.network.result.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ServerManagementUiState(
    val isLoading: Boolean = true,
    val data: ServerManageData? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val actionError: String? = null,
)

class ServerManagementViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val repository = ServiceLocator.serverRepository

    private val _state = MutableStateFlow(ServerManagementUiState())
    val state: StateFlow<ServerManagementUiState> = _state.asStateFlow()

    private var serverAddress: String = ""

    init {
        loadServer()
    }

    private fun loadServer() {
        val server = repository.getServerById(serverId)
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

    fun save(name: String, username: String, description: String, imageUrl: String) {
        if (serverAddress.isEmpty()) return
        _state.value = _state.value.copy(isSaving = true, actionError = null)
        viewModelScope.launch {
            when (val result = repository.updateServer(serverAddress, name, username, description, imageUrl)) {
                is ApiResult.Success -> {
                    try {
                        ServiceLocator.sessionStore.updateServerMetadata(
                            serverAddress = serverAddress,
                            serverId = result.data.id,
                            serverName = result.data.name,
                            serverUsername = result.data.username,
                        )
                    } catch (_: UninitializedPropertyAccessException) {
                        // Ignore in previews/tests.
                    }
                    _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
                }
                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        actionError = "Не удалось сохранить изменения",
                    )
                }
            }
        }
    }

    fun deleteServer() {
        if (serverAddress.isEmpty()) return
        _state.value = _state.value.copy(isDeleting = true, actionError = null)
        viewModelScope.launch {
            when (repository.deleteServer(serverAddress)) {
                is ApiResult.Success -> {
                    try {
                        ServiceLocator.sessionStore.removeSession(serverAddress)
                    } catch (_: UninitializedPropertyAccessException) {
                        // Ignore in previews/tests.
                    }
                    if (ServiceLocator.activeServerAddress == serverAddress) {
                        ServiceLocator.activeServerAddress = ""
                        ServiceLocator.currentUserId = ""
                    }
                    _state.value = _state.value.copy(isDeleting = false, deleteSuccess = true)
                }
                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        actionError = "Не удалось удалить сервер",
                    )
                }
            }
        }
    }

    fun clearActionError() {
        _state.value = _state.value.copy(actionError = null)
    }
}
