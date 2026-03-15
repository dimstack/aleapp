package com.callapp.android.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callapp.android.data.ServiceLocator
import com.callapp.android.domain.model.Server
import com.callapp.android.network.CreateUserResult
import com.callapp.android.network.InviteTokenParser
import com.callapp.android.network.dto.AuthResponse
import com.callapp.android.network.dto.ConnectResponse
import com.callapp.android.network.dto.toDomain
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.apiErrorMessage
import com.callapp.android.ui.common.localizeBackendMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val INVALID_TOKEN_FORMAT = "\u041D\u0435\u0432\u0435\u0440\u043D\u044B\u0439\u0020\u0444\u043E\u0440\u043C\u0430\u0442\u0020\u0442\u043E\u043A\u0435\u043D\u0430"
private const val SERVER_NOT_DEFINED = "\u0421\u0435\u0440\u0432\u0435\u0440\u0020\u043D\u0435\u0020\u043E\u043F\u0440\u0435\u0434\u0435\u043B\u0435\u043D"
private const val PASSWORD_TOO_SHORT = "\u041F\u0430\u0440\u043E\u043B\u044C\u0020\u0434\u043E\u043B\u0436\u0435\u043D\u0020\u0441\u043E\u0434\u0435\u0440\u0436\u0430\u0442\u044C\u0020\u043C\u0438\u043D\u0438\u043C\u0443\u043C\u0020\u0038\u0020\u0441\u0438\u043C\u0432\u043E\u043B\u043E\u0432"
private const val PROFILE_CREATED_BUT_SESSION_FAILED = "\u041F\u0440\u043E\u0444\u0438\u043B\u044C\u0020\u0441\u043E\u0437\u0434\u0430\u043D\u002C\u0020\u043D\u043E\u0020\u043D\u0435\u0020\u0443\u0434\u0430\u043B\u043E\u0441\u044C\u0020\u043E\u0442\u043A\u0440\u044B\u0442\u044C\u0020\u0441\u0435\u0441\u0441\u0438\u044E\u002E\u0020\u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435\u0020\u0432\u043E\u0439\u0442\u0438\u0020\u0432\u0440\u0443\u0447\u043D\u0443\u044E\u002E"
private const val NETWORK_ERROR_MESSAGE = "\u041D\u0435\u0442\u0020\u0441\u043E\u0435\u0434\u0438\u043D\u0435\u043D\u0438\u044F\u0020\u0441\u0020\u0441\u0435\u0440\u0432\u0435\u0440\u043E\u043C"
private const val SERVER_NOT_FOUND_MESSAGE = "\u0421\u0435\u0440\u0432\u0435\u0440\u0020\u043D\u0435\u0020\u043D\u0430\u0439\u0434\u0435\u043D"
private const val SERVER_ERROR_MESSAGE = "\u041E\u0448\u0438\u0431\u043A\u0430\u0020\u0441\u0435\u0440\u0432\u0435\u0440\u0430"
private const val INVALID_CREDENTIALS_MESSAGE = "\u041D\u0435\u0432\u0435\u0440\u043D\u044B\u0439\u0020\u0075\u0073\u0065\u0072\u006E\u0061\u006D\u0065\u0020\u0438\u043B\u0438\u0020\u043F\u0430\u0440\u043E\u043B\u044C"
private const val SESSION_EXPIRED_MESSAGE = "\u0421\u0435\u0441\u0441\u0438\u044F\u0020\u043F\u043E\u0434\u043A\u043B\u044E\u0447\u0435\u043D\u0438\u044F\u0020\u0438\u0441\u0442\u0435\u043A\u043B\u0430\u002E\u0020\u041F\u043E\u0434\u043A\u043B\u044E\u0447\u0438\u0442\u0435\u0441\u044C\u0020\u0437\u0430\u043D\u043E\u0432\u043E"
private const val CREATE_PROFILE_FAILED_MESSAGE = "\u041D\u0435\u0020\u0443\u0434\u0430\u043B\u043E\u0441\u044C\u0020\u0441\u043E\u0437\u0434\u0430\u0442\u044C\u0020\u043F\u0440\u043E\u0444\u0438\u043B\u044C"
private const val LOGIN_LOCKED_MESSAGE = "\u0421\u043B\u0438\u0448\u043A\u043E\u043C\u0020\u043C\u043D\u043E\u0433\u043E\u0020\u043F\u043E\u043F\u044B\u0442\u043E\u043A\u0020\u0432\u0445\u043E\u0434\u0430\u002E\u0020\u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435\u0020\u0447\u0435\u0440\u0435\u0437\u0020\u0031\u0035\u0020\u043C\u0438\u043D\u0443\u0442\u002E"
private const val INVITE_TOKEN_INVALID_MESSAGE = "\u0422\u043E\u043A\u0435\u043D\u0020\u043F\u0440\u0438\u0433\u043B\u0430\u0448\u0435\u043D\u0438\u044F\u0020\u043D\u0435\u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0442\u0435\u043B\u0435\u043D"
private const val INVITE_TOKEN_REVOKED_MESSAGE = "\u0422\u043E\u043A\u0435\u043D\u0020\u043F\u0440\u0438\u0433\u043B\u0430\u0448\u0435\u043D\u0438\u044F\u0020\u043E\u0442\u043E\u0437\u0432\u0430\u043D"
private const val INVITE_TOKEN_EXHAUSTED_MESSAGE = "\u041B\u0438\u043C\u0438\u0442\u0020\u0438\u0441\u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u043D\u0438\u044F\u0020\u0442\u043E\u043A\u0435\u043D\u0430\u0020\u0438\u0441\u0447\u0435\u0440\u043F\u0430\u043D"
private const val INVITE_TOKEN_GENERIC_MESSAGE = "\u0422\u043E\u043A\u0435\u043D\u0020\u043D\u0435\u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0442\u0435\u043B\u0435\u043D\u0020\u0438\u043B\u0438\u0020\u0438\u0441\u0442\u0435\u043A"

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

interface ConnectDependencies {
    fun parseInviteToken(rawToken: String): Pair<String, String>?
    suspend fun connect(serverAddress: String, inviteToken: String): ApiResult<ConnectResponse>
    suspend fun createUser(
        serverAddress: String,
        name: String,
        username: String,
        password: String,
    ): ApiResult<CreateUserResult>

    suspend fun login(
        serverAddress: String,
        inviteToken: String,
        username: String,
        password: String,
    ): ApiResult<AuthResponse>

    fun restoreSession(serverAddress: String, sessionToken: String)

    fun saveSession(
        serverAddress: String,
        sessionToken: String,
        userId: String,
        server: Server?,
    )

    fun savePendingApproval(
        serverAddress: String,
        inviteToken: String,
        username: String,
        password: String,
        serverName: String,
    )

    fun removePendingApproval(serverAddress: String)
}

object DefaultConnectDependencies : ConnectDependencies {
    override fun parseInviteToken(rawToken: String): Pair<String, String>? {
        val parsed = InviteTokenParser.parse(rawToken) ?: return null
        return parsed.serverAddress to parsed.code
    }

    override suspend fun connect(serverAddress: String, inviteToken: String): ApiResult<ConnectResponse> =
        ServiceLocator.serverRepository.connect(serverAddress, inviteToken)

    override suspend fun createUser(
        serverAddress: String,
        name: String,
        username: String,
        password: String,
    ): ApiResult<CreateUserResult> =
        ServiceLocator.connectionManager.getClient(serverAddress).createUser(
            name = name,
            username = username,
            password = password,
        )

    override suspend fun login(
        serverAddress: String,
        inviteToken: String,
        username: String,
        password: String,
    ): ApiResult<AuthResponse> =
        ServiceLocator.serverRepository.login(
            serverAddress = serverAddress,
            inviteToken = inviteToken,
            username = username,
            password = password,
        )

    override fun restoreSession(serverAddress: String, sessionToken: String) {
        ServiceLocator.activeServerAddress = serverAddress
        ServiceLocator.connectionManager.restoreSession(serverAddress, sessionToken)
    }

    override fun saveSession(
        serverAddress: String,
        sessionToken: String,
        userId: String,
        server: Server?,
    ) {
        ServiceLocator.activeServerAddress = serverAddress
        ServiceLocator.currentUserId = userId
        ServiceLocator.sessionStore.saveSession(
            serverAddress = serverAddress,
            sessionToken = sessionToken,
            userId = userId,
            serverName = server?.name.orEmpty(),
            serverUsername = server?.username.orEmpty(),
            serverId = server?.id.orEmpty(),
        )
    }

    override fun savePendingApproval(
        serverAddress: String,
        inviteToken: String,
        username: String,
        password: String,
        serverName: String,
    ) {
        ServiceLocator.sessionStore.savePendingApproval(
            serverAddress = serverAddress,
            inviteToken = inviteToken,
            username = username,
            password = password,
            serverName = serverName,
        )
    }

    override fun removePendingApproval(serverAddress: String) {
        ServiceLocator.sessionStore.removePendingApproval(serverAddress)
    }
}

class ConnectViewModel(
    private val dependencies: ConnectDependencies = DefaultConnectDependencies,
) : ViewModel() {

    private val _state = MutableStateFlow<ConnectUiState>(ConnectUiState.Idle)
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    var currentServerAddress: String = ""
        private set
    var currentTokenCode: String = ""
        private set
    var currentServerName: String = ""
        private set

    fun connectWithToken(rawToken: String) {
        val parsed = dependencies.parseInviteToken(rawToken)
        if (parsed == null) {
            _state.value = ConnectUiState.Error(INVALID_TOKEN_FORMAT)
            return
        }
        val (serverAddress, tokenCode) = parsed

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            when (val result = dependencies.connect(serverAddress, tokenCode)) {
                is ApiResult.Success -> {
                    val response = result.data
                    currentServerAddress = serverAddress
                    currentTokenCode = tokenCode
                    currentServerName = response.server?.name ?: "Server"

                    when {
                        response.isPending -> {
                            savePendingApproval(
                                serverAddress = serverAddress,
                                username = response.user?.username ?: "",
                                password = "",
                            )
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
                    _state.value = ConnectUiState.Error(connectErrorMessage(result.error))
                }
            }
        }
    }

    fun createProfile(username: String, name: String, password: String) {
        if (currentServerAddress.isEmpty()) {
            _state.value = ConnectUiState.Error(SERVER_NOT_DEFINED)
            return
        }
        if (password.length < 8) {
            _state.value = ConnectUiState.Error(PASSWORD_TOO_SHORT)
            return
        }

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            when (
                val result = dependencies.createUser(
                    serverAddress = currentServerAddress,
                    name = name,
                    username = username,
                    password = password,
                )
            ) {
                is ApiResult.Success -> {
                    when (val createResult = result.data) {
                        is CreateUserResult.Pending -> {
                            savePendingApproval(
                                serverAddress = currentServerAddress,
                                username = createResult.response.user?.username ?: normalizeUsername(username),
                                password = password,
                            )
                            _state.value = ConnectUiState.Pending(
                                serverAddress = currentServerAddress,
                                serverName = currentServerName,
                                userName = createResult.response.user?.username ?: normalizeUsername(username),
                            )
                        }

                        is CreateUserResult.Joined -> {
                            when (
                                val loginResult = dependencies.login(
                                    serverAddress = currentServerAddress,
                                    inviteToken = currentTokenCode,
                                    username = username,
                                    password = password,
                                )
                            ) {
                                is ApiResult.Success -> {
                                    val response = loginResult.data
                                    if (response.isPending) {
                                        savePendingApproval(
                                            serverAddress = currentServerAddress,
                                            username = response.user?.username ?: normalizeUsername(username),
                                            password = password,
                                        )
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
                                    _state.value = ConnectUiState.Error(PROFILE_CREATED_BUT_SESSION_FAILED)
                                }
                            }
                        }
                    }
                }

                is ApiResult.Failure -> {
                    _state.value = ConnectUiState.Error(createProfileErrorMessage(result.error))
                }
            }
        }
    }

    fun login(username: String, password: String) {
        if (currentServerAddress.isEmpty() || currentTokenCode.isEmpty()) {
            _state.value = ConnectUiState.Error(SERVER_NOT_DEFINED)
            return
        }

        _state.value = ConnectUiState.Loading

        viewModelScope.launch {
            when (
                val result = dependencies.login(
                    serverAddress = currentServerAddress,
                    inviteToken = currentTokenCode,
                    username = username,
                    password = password,
                )
            ) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.isPending) {
                        savePendingApproval(
                            serverAddress = currentServerAddress,
                            username = response.user?.username ?: normalizeUsername(username),
                            password = password,
                        )
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
                    _state.value = ConnectUiState.LoginError(loginErrorMessage(result.error))
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
        dependencies.restoreSession(serverAddress, sessionToken)
        try {
            dependencies.saveSession(
                serverAddress = serverAddress,
                sessionToken = sessionToken,
                userId = userId,
                server = server,
            )
            dependencies.removePendingApproval(serverAddress)
        } catch (_: UninitializedPropertyAccessException) {
            // SessionStore is not initialized yet. This should not happen in normal flow.
        }
    }

    private fun savePendingApproval(
        serverAddress: String,
        username: String,
        password: String,
    ) {
        if (serverAddress.isBlank() || currentTokenCode.isBlank() || username.isBlank() || password.isBlank()) return
        try {
            dependencies.savePendingApproval(
                serverAddress = serverAddress,
                inviteToken = currentTokenCode,
                username = username,
                password = password,
                serverName = currentServerName,
            )
        } catch (_: UninitializedPropertyAccessException) {
            // Ignore in previews/tests.
        }
    }
}

internal fun connectErrorMessage(error: ApiError): String = when (error) {
    ApiError.NetworkError -> NETWORK_ERROR_MESSAGE
    ApiError.NotFound -> SERVER_NOT_FOUND_MESSAGE
    is ApiError.Unauthorized -> inviteTokenErrorMessage(error)
    else -> apiErrorMessage(
        error = error,
        fallback = SERVER_ERROR_MESSAGE,
        notFound = SERVER_NOT_FOUND_MESSAGE,
        unauthorized = INVITE_TOKEN_GENERIC_MESSAGE,
    )
}

internal fun createProfileErrorMessage(error: ApiError): String = when (error) {
    is ApiError.Unauthorized -> SESSION_EXPIRED_MESSAGE
    else -> apiErrorMessage(
        error = error,
        fallback = CREATE_PROFILE_FAILED_MESSAGE,
        notFound = SERVER_NOT_FOUND_MESSAGE,
        unauthorized = SESSION_EXPIRED_MESSAGE,
    )
}

internal fun loginErrorMessage(error: ApiError): String = when (error) {
    ApiError.NetworkError -> NETWORK_ERROR_MESSAGE
    ApiError.NotFound -> SERVER_NOT_FOUND_MESSAGE
    is ApiError.LoginLocked -> LOGIN_LOCKED_MESSAGE
    is ApiError.Unauthorized -> {
        when (error.code) {
            "invite_token_invalid", "invite_token_revoked", "invite_token_exhausted" -> inviteTokenErrorMessage(error)
            else -> INVALID_CREDENTIALS_MESSAGE
        }
    }

    else -> apiErrorMessage(
        error = error,
        fallback = SERVER_ERROR_MESSAGE,
        notFound = SERVER_NOT_FOUND_MESSAGE,
        unauthorized = INVALID_CREDENTIALS_MESSAGE,
    )
}

private fun inviteTokenErrorMessage(error: ApiError.Unauthorized): String = when (error.code) {
    "invite_token_invalid" -> INVITE_TOKEN_INVALID_MESSAGE
    "invite_token_revoked" -> INVITE_TOKEN_REVOKED_MESSAGE
    "invite_token_exhausted" -> INVITE_TOKEN_EXHAUSTED_MESSAGE
    else -> localizeBackendMessage(error.message) ?: INVITE_TOKEN_GENERIC_MESSAGE
}
