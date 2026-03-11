package com.example.android.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.data.ServerRepository
import com.example.android.domain.model.Server
import com.example.android.network.ServerConnectionManager
import com.example.android.network.result.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConnectUiState {
    data object Idle : ConnectUiState()
    data object Loading : ConnectUiState()
    data class AuthChoice(
        val serverAddress: String,
        val serverName: String,
        val tokenCode: String,
    ) : ConnectUiState()
    data class NeedsProfile(val serverAddress: String, val serverName: String) : ConnectUiState()
    data class Joined(val serverAddress: String) : ConnectUiState()
    data class Pending(val serverAddress: String, val serverName: String) : ConnectUiState()
    data class LoginError(val message: String) : ConnectUiState()
    data class Error(val message: String) : ConnectUiState()
}

class ConnectViewModel(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ConnectUiState>(ConnectUiState.Idle)
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    /** Текущий адрес сервера и токен для повторного использования в login. */
    private var currentServerAddress: String = ""
    private var currentTokenCode: String = ""

    fun connectWithToken(rawToken: String) {
        val parsed = ServerConnectionManager.parseInviteToken(rawToken)
        if (parsed == null) {
            _state.value = ConnectUiState.Error("Неверный формат токена")
            return
        }
        val (serverAddress, tokenCode) = parsed

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            when (val result = repository.auth(serverAddress, tokenCode, "")) {
                is ApiResult.Success -> {
                    val response = result.data
                    currentServerAddress = serverAddress
                    currentTokenCode = tokenCode
                    when {
                        response.isPending -> {
                            _state.value = ConnectUiState.Pending(
                                serverAddress = serverAddress,
                                serverName = "Server",
                            )
                        }
                        response.isJoined && response.user != null -> {
                            _state.value = ConnectUiState.Joined(serverAddress = serverAddress)
                        }
                        else -> {
                            _state.value = ConnectUiState.AuthChoice(
                                serverAddress = serverAddress,
                                serverName = "Server",
                                tokenCode = tokenCode,
                            )
                        }
                    }
                }
                is ApiResult.Failure -> {
                    _state.value = ConnectUiState.Error("Не удалось подключиться к серверу")
                }
            }
        }
    }

    /** Вход в существующий аккаунт. */
    fun login(username: String, password: String) {
        if (currentServerAddress.isEmpty() || currentTokenCode.isEmpty()) {
            _state.value = ConnectUiState.Error("Сервер не определён")
            return
        }

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            when (val result = repository.login(
                serverAddress = currentServerAddress,
                inviteToken = currentTokenCode,
                username = username,
                password = password,
            )) {
                is ApiResult.Success -> {
                    _state.value = ConnectUiState.Joined(serverAddress = currentServerAddress)
                }
                is ApiResult.Failure -> {
                    _state.value = ConnectUiState.LoginError(
                        "Неверный username или пароль",
                    )
                }
            }
        }
    }

    fun resetState() {
        _state.value = ConnectUiState.Idle
    }
}
