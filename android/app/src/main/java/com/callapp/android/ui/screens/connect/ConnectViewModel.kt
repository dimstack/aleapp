package com.callapp.android.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Server
import com.callapp.android.network.CreateUserResult
import com.callapp.android.network.ServerConnectionManager
import com.callapp.android.network.dto.toDomain
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
            when (val result = repo.connect(serverAddress, tokenCode)) {
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
                            saveSession(
                                serverAddress = serverAddress,
                                sessionToken = response.sessionToken,
                                userId = response.user.id,
                                server = response.server?.toDomain(serverAddress),
                            )
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
                        ApiError.Unauthorized -> "Токен недействителен или истек"
                        ApiError.ServerError -> "Ошибка сервера"
                    }
                    _state.value = ConnectUiState.Error(message)
                }
            }
        }
    }

    fun createProfile(username: String, name: String, password: String) {
        if (currentServerAddress.isEmpty()) {
            _state.value = ConnectUiState.Error("Сервер не определен")
            return
        }

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            val client = connectionManager.getClient(currentServerAddress)
            when (
                val result = client.createUser(
                    name = name,
                    username = username,
                    password = password,
                )
            ) {
                is ApiResult.Success -> {
                    when (val createResult = result.data) {
                        is CreateUserResult.Pending -> {
                            _state.value = ConnectUiState.Pending(
                                serverAddress = currentServerAddress,
                                serverName = currentServerName,
                                userName = createResult.response.user?.username ?: normalizeUsername(username),
                            )
                        }

                        is CreateUserResult.Joined -> {
                            when (
                                val loginResult = repo.login(
                                    serverAddress = currentServerAddress,
                                    inviteToken = currentTokenCode,
                                    username = username,
                                    password = password,
                                )
                            ) {
                                is ApiResult.Success -> {
                                    val response = loginResult.data
                                    if (response.isPending) {
                                        _state.value = ConnectUiState.Pending(
                                            serverAddress = currentServerAddress,
                                            serverName = currentServerName,
                                            userName = response.user?.username ?: normalizeUsername(username),
                                        )
                                    } else {
                                        val server = response.server?.toDomain(currentServerAddress) ?: Server(
                                            id = currentServerAddress,
                                            name = currentServerName.ifBlank { currentServerAddress },
                                            username = "",
                                            address = currentServerAddress,
                                        )
                                        saveSession(
                                            serverAddress = currentServerAddress,
                                            sessionToken = response.sessionToken,
                                            userId = response.user?.id ?: createResult.user.id,
                                            server = server,
                                        )
                                        _state.value = ConnectUiState.Joined(
                                            serverAddress = currentServerAddress,
                                            serverName = currentServerName,
                                        )
                                    }
                                }

                                is ApiResult.Failure -> {
                                    _state.value = ConnectUiState.Error(
                                        "Профиль создан, но не удалось открыть сессию. Попробуйте войти вручную.",
                                    )
                                }
                            }
                        }
                    }
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

    fun login(username: String, password: String) {
        if (currentServerAddress.isEmpty() || currentTokenCode.isEmpty()) {
            _state.value = ConnectUiState.Error("Сервер не определен")
            return
        }

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            when (
                val result = repo.login(
                    serverAddress = currentServerAddress,
                    inviteToken = currentTokenCode,
                    username = username,
                    password = password,
                )
            ) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.isPending) {
                        _state.value = ConnectUiState.Pending(
                            serverAddress = currentServerAddress,
                            serverName = currentServerName,
                            userName = response.user?.username ?: normalizeUsername(username),
                        )
                    } else {
                        saveSession(
                            serverAddress = currentServerAddress,
                            sessionToken = response.sessionToken,
                            userId = response.user?.id ?: "",
                            server = response.server?.toDomain(currentServerAddress),
                        )
                        _state.value = ConnectUiState.Joined(
                            serverAddress = currentServerAddress,
                            serverName = currentServerName,
                        )
                    }
                }

                is ApiResult.Failure -> {
                    _state.value = ConnectUiState.LoginError("Неверный username или пароль")
                }
            }
        }
    }

    fun resetState() {
        _state.value = ConnectUiState.Idle
    }

    private fun normalizeUsername(username: String): String {
        val trimmed = username.trim()
        return if (trimmed.startsWith("@")) trimmed else "@$trimmed"
    }

    private fun saveSession(
        serverAddress: String,
        sessionToken: String,
        userId: String,
        server: Server? = null,
    ) {
        ServiceLocator.activeServerAddress = serverAddress
        ServiceLocator.currentUserId = userId
        connectionManager.restoreSession(serverAddress, sessionToken)
        try {
            ServiceLocator.sessionStore.saveSession(
                serverAddress = serverAddress,
                sessionToken = sessionToken,
                userId = userId,
                serverName = server?.name.orEmpty(),
                serverUsername = server?.username.orEmpty(),
                serverId = server?.id.orEmpty(),
            )
        } catch (_: UninitializedPropertyAccessException) {
            // SessionStore is not initialized yet. This should not happen in normal flow.
        }
    }
}
