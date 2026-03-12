package com.callapp.android.ui.screens.servermanage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServerRepository
import com.callapp.android.domain.model.InviteToken
import com.callapp.android.network.result.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InviteTokensUiState {
    data object Loading : InviteTokensUiState()
    data class Success(val tokens: List<InviteToken>) : InviteTokensUiState()
    data class Error(val message: String) : InviteTokensUiState()
}

class InviteTokensViewModel(
    private val serverAddress: String,
    private val repository: ServerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<InviteTokensUiState>(InviteTokensUiState.Loading)
    val state: StateFlow<InviteTokensUiState> = _state.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    init {
        loadTokens()
    }

    fun loadTokens() {
        _state.value = InviteTokensUiState.Loading
        viewModelScope.launch {
            when (val result = repository.getInviteTokens(serverAddress)) {
                is ApiResult.Success -> {
                    _state.value = InviteTokensUiState.Success(result.data)
                }
                is ApiResult.Failure -> {
                    _state.value = InviteTokensUiState.Error("Не удалось загрузить токены")
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
            when (repository.createInviteToken(serverAddress, label, maxUses, grantedRole, requireApproval)) {
                is ApiResult.Success -> loadTokens()
                is ApiResult.Failure -> {
                    // Token creation failed, state stays the same
                }
            }
            _isCreating.value = false
        }
    }

    fun revokeToken(tokenId: String) {
        viewModelScope.launch {
            when (repository.revokeInviteToken(serverAddress, tokenId)) {
                is ApiResult.Success -> loadTokens()
                is ApiResult.Failure -> {
                    // Revoke failed
                }
            }
        }
    }
}
