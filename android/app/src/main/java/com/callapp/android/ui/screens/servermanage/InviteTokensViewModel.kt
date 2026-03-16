package com.callapp.android.ui.screens.servermanage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.InviteToken
import com.callapp.android.domain.model.Server
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.apiErrorMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

sealed interface InviteTokensEvent {
    data class CopyTokenToClipboard(val fullToken: String) : InviteTokensEvent
}

interface InviteTokensDependencies {
    fun getServerById(serverId: String): Server?
    suspend fun getInviteTokens(serverAddress: String): ApiResult<List<InviteToken>>
    suspend fun createInviteToken(
        serverAddress: String,
        label: String,
        maxUses: Int,
        grantedRole: String,
        requireApproval: Boolean,
    ): ApiResult<InviteToken>

    suspend fun revokeInviteToken(serverAddress: String, tokenId: String): ApiResult<Unit>
}

object DefaultInviteTokensDependencies : InviteTokensDependencies {
    private val repository get() = ServiceLocator.serverRepository

    override fun getServerById(serverId: String): Server? = repository.getServerById(serverId)

    override suspend fun getInviteTokens(serverAddress: String): ApiResult<List<InviteToken>> =
        repository.getInviteTokens(serverAddress)

    override suspend fun createInviteToken(
        serverAddress: String,
        label: String,
        maxUses: Int,
        grantedRole: String,
        requireApproval: Boolean,
    ): ApiResult<InviteToken> =
        repository.createInviteToken(serverAddress, label, maxUses, grantedRole, requireApproval)

    override suspend fun revokeInviteToken(serverAddress: String, tokenId: String): ApiResult<Unit> =
        repository.revokeInviteToken(serverAddress, tokenId)
}

class InviteTokensViewModel(
    savedStateHandle: SavedStateHandle,
    private val dependencies: InviteTokensDependencies = DefaultInviteTokensDependencies,
) : ViewModel() {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle = savedStateHandle,
        dependencies = DefaultInviteTokensDependencies,
    )

    private val serverId: String = savedStateHandle["serverId"] ?: ""
    private val serverAddress: String = dependencies.getServerById(serverId)?.address.orEmpty()

    private val _state = MutableStateFlow<InviteTokensUiState>(InviteTokensUiState.Loading)
    val state: StateFlow<InviteTokensUiState> = _state.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _events = MutableSharedFlow<InviteTokensEvent>()
    val events: SharedFlow<InviteTokensEvent> = _events.asSharedFlow()

    val currentServerAddress: String
        get() = serverAddress

    init {
        loadTokens()
    }

    fun loadTokens() {
        if (serverAddress.isEmpty()) {
            _state.value = InviteTokensUiState.Error("Сервер не найден")
            return
        }
        _state.value = InviteTokensUiState.Loading
        viewModelScope.launch {
            when (val result = dependencies.getInviteTokens(serverAddress)) {
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
        if (label.isBlank()) {
            setActionError("Название токена обязательно")
            return
        }

        _isCreating.value = true
        viewModelScope.launch {
            when (
                val result = dependencies.createInviteToken(
                    serverAddress = serverAddress,
                    label = label.trim(),
                    maxUses = maxUses,
                    grantedRole = grantedRole,
                    requireApproval = requireApproval,
                )
            ) {
                is ApiResult.Success -> {
                    appendToken(result.data)
                }

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
            when (val result = dependencies.revokeInviteToken(serverAddress, tokenId)) {
                is ApiResult.Success -> {
                    val current = _state.value
                    if (current is InviteTokensUiState.Success) {
                        _state.value = current.copy(
                            tokens = current.tokens.filterNot { it.id == tokenId },
                        )
                    }
                }

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

    fun copyTokenToClipboard(fullToken: String) {
        viewModelScope.launch {
            _events.emit(InviteTokensEvent.CopyTokenToClipboard(fullToken))
        }
    }

    fun clearActionError() {
        val current = _state.value
        if (current is InviteTokensUiState.Success) {
            _state.value = current.copy(actionError = null)
        }
    }

    private fun appendToken(token: InviteToken) {
        val current = _state.value
        if (current is InviteTokensUiState.Success) {
            _state.value = current.copy(tokens = listOf(token) + current.tokens, actionError = null)
        } else {
            _state.value = InviteTokensUiState.Success(listOf(token))
        }
    }

    private fun setActionError(message: String) {
        val current = _state.value
        if (current is InviteTokensUiState.Success) {
            _state.value = current.copy(actionError = message)
        } else {
            _state.value = InviteTokensUiState.Success(tokens = emptyList(), actionError = message)
        }
    }
}
