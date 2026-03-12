package com.callapp.android.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.network.ServerConnectionManager
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
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
    data class Joined(val serverAddress: String, val serverName: String) : ConnectUiState()
    data class Pending(
        val serverAddress: String,
        val serverName: String,
        val userName: String,
    ) : ConnectUiState()
    data class LoginError(val message: String) : ConnectUiState()
    data class Error(val message: String) : ConnectUiState()
}

class ConnectViewModel : ViewModel() {

    private val repo = ServiceLocator.serverRepository
    private val connectionManager = ServiceLocator.connectionManager

    private val _state = MutableStateFlow<ConnectUiState>(ConnectUiState.Idle)
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    /** Current server address and token for reuse in login/create. */
    var currentServerAddress: String = ""
        private set
    var currentTokenCode: String = ""
        private set
    var currentServerName: String = ""
        private set

    fun connectWithToken(rawToken: String) {
        val parsed = ServerConnectionManager.parseInviteToken(rawToken)
        if (parsed == null) {
            _state.value = ConnectUiState.Error("Неверный формат токена")
            return
        }
        val (serverAddress, tokenCode) = parsed

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            when (val result = repo.auth(serverAddress, tokenCode, "")) {
                is ApiResult.Success -> {
                    val response = result.data
                    currentServerAddress = serverAddress
                    currentTokenCode = tokenCode
                    currentServerName = response.server?.name ?: "Server"

                    when {
                        response.isPending -> {
                            _state.value = ConnectUiState.Pending(
                                serverAddress = serverAddress,
                                serverName = currentServerName,
                                userName = response.user?.username ?: "",
                            )
                        }
                        response.isJoined && response.user != null -> {
                            // Already a member — save session and go home
                            saveSession(serverAddress, response.sessionToken, response.user.id)
                            _state.value = ConnectUiState.Joined(
                                serverAddress = serverAddress,
                                serverName = currentServerName,
                            )
                        }
                        response.needsProfile -> {
                            _state.value = ConnectUiState.AuthChoice(
                                serverAddress = serverAddress,
                                serverName = currentServerName,
                                tokenCode = tokenCode,
                            )
                        }
                        else -> {
                            // Default: show auth choice (register / login)
                            _state.value = ConnectUiState.AuthChoice(
                                serverAddress = serverAddress,
                                serverName = currentServerName,
                                tokenCode = tokenCode,
                            )
                        }
                    }
                }
                is ApiResult.Failure -> {
                    val message = when (result.error) {
                        ApiError.NetworkError -> "Нет соединения с сервером"
                        ApiError.NotFound -> "Сервер не найден"
                        ApiError.Unauthorized -> "Токен недействителен или истёк"
                        ApiError.ServerError -> "Ошибка сервера"
                    }
                    _state.value = ConnectUiState.Error(message)
                }
            }
        }
    }

    /** Create a new account on the server. */
    fun createProfile(username: String, name: String, password: String) {
        if (currentServerAddress.isEmpty()) {
            _state.value = ConnectUiState.Error("Сервер не определён")
            return
        }

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            val client = connectionManager.getClient(currentServerAddress)
            when (val result = client.createUser(
                name = name,
                username = username,
                password = password,
            )) {
                is ApiResult.Success -> {
                    val user = result.data
                    val token = client.sessionToken ?: ""
                    saveSession(currentServerAddress, token, user.id)
                    _state.value = ConnectUiState.Joined(
                        serverAddress = currentServerAddress,
                        serverName = currentServerName,
                    )
                }
                is ApiResult.Failure -> {
                    val message = when (result.error) {
                        ApiError.NetworkError -> "Нет соединения с сервером"
                        ApiError.Unauthorized -> "Сессия истекла, подключитесь заново"
                        else -> "Не удалось создать профиль"
                    }
                    _state.value = ConnectUiState.Error(message)
                }
            }
        }
    }

    /** Login with existing credentials. */
    fun login(username: String, password: String) {
        if (currentServerAddress.isEmpty() || currentTokenCode.isEmpty()) {
            _state.value = ConnectUiState.Error("Сервер не определён")
            return
        }

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            when (val result = repo.login(
                serverAddress = currentServerAddress,
                inviteToken = currentTokenCode,
                username = username,
                password = password,
            )) {
                is ApiResult.Success -> {
                    val response = result.data
                    saveSession(
                        currentServerAddress,
                        response.sessionToken,
                        response.user?.id ?: "",
                    )
                    _state.value = ConnectUiState.Joined(
                        serverAddress = currentServerAddress,
                        serverName = currentServerName,
                    )
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

    private fun saveSession(serverAddress: String, sessionToken: String, userId: String) {
        ServiceLocator.activeServerAddress = serverAddress
        ServiceLocator.currentUserId = userId
        connectionManager.restoreSession(serverAddress, sessionToken)
        // Persist session for app restart
        try {
            ServiceLocator.sessionStore.saveSession(serverAddress, sessionToken, userId)
        } catch (_: UninitializedPropertyAccessException) {
            // SessionStore not yet initialized (shouldn't happen in normal flow)
        }
    }
}
