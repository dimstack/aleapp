package com.callapp.android.ui.screens.servermanage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.InviteToken
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.apiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InviteTokensUiState {
    data object Loading : InviteTokensUiState()
    data class Success(
        val tokens: List<InviteToken>,
        val actionError: String? = null,
    ) : InviteTokensUiState()

    data class Error(val message: String) : InviteTokensUiState()
}

class InviteTokensViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val repository = ServiceLocator.serverRepository

    private val serverAddress: String

    private val _state = MutableStateFlow<InviteTokensUiState>(InviteTokensUiState.Loading)
    val state: StateFlow<InviteTokensUiState> = _state.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    val currentServerAddress: String
        get() = serverAddress

    init {
        val server = repository.getServerById(serverId)
        serverAddress = server?.address ?: ""
        loadTokens()
    }

    fun loadTokens() {
        if (serverAddress.isEmpty()) {
            _state.value = InviteTokensUiState.Error("Сервер не найден")
            return
        }
        _state.value = InviteTokensUiState.Loading
        viewModelScope.launch {
            when (val result = repository.getInviteTokens(serverAddress)) {
                is ApiResult.Success -> {
                    _state.value = InviteTokensUiState.Success(result.data)
                }

                is ApiResult.Failure -> {
                    _state.value = InviteTokensUiState.Error(
                        apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось загрузить токены",
                            notFound = "Сервер не найден",
                        ),
                    )
                }
            }
        }
    }

    fun createToken(
        label: String,
        maxUses: Int,
        grantedRole: String,
        requireApproval: Boolean,
    ) {
        _isCreating.value = true
        viewModelScope.launch {
            when (val result = repository.createInviteToken(serverAddress, label, maxUses, grantedRole, requireApproval)) {
                is ApiResult.Success -> loadTokens()
                is ApiResult.Failure -> {
                    setActionError(
                        apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось создать токен",
                            notFound = "Сервер не найден",
                        ),
                    )
                }
            }
            _isCreating.value = false
        }
    }

    fun revokeToken(tokenId: String) {
        viewModelScope.launch {
            when (val result = repository.revokeInviteToken(serverAddress, tokenId)) {
                is ApiResult.Success -> loadTokens()
                is ApiResult.Failure -> {
                    setActionError(
                        apiErrorMessage(
                            error = result.error,
                            fallback = "Не удалось отозвать токен",
                            notFound = "Токен не найден",
                        ),
                    )
                }
            }
        }
    }

    fun clearActionError() {
        val current = _state.value
        if (current is InviteTokensUiState.Success) {
            _state.value = current.copy(actionError = null)
        }
    }

    private fun setActionError(message: String) {
        val current = _state.value
        if (current is InviteTokensUiState.Success) {
            _state.value = current.copy(actionError = message)
        }
    }
}
