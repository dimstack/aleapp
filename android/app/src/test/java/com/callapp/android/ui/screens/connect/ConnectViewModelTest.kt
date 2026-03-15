package com.callapp.android.ui.screens.connect

import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.network.CreateUserResult
import com.callapp.android.network.dto.AuthResponse
import com.callapp.android.network.dto.ConnectResponse
import com.callapp.android.network.dto.ServerDto
import com.callapp.android.network.dto.UserDto
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectViewModelTest {

    companion object {
        private const val INVALID_TOKEN_FORMAT = "\u041D\u0435\u0432\u0435\u0440\u043D\u044B\u0439\u0020\u0444\u043E\u0440\u043C\u0430\u0442\u0020\u0442\u043E\u043A\u0435\u043D\u0430"
        private const val NETWORK_ERROR_MESSAGE = "\u041D\u0435\u0442\u0020\u0441\u043E\u0435\u0434\u0438\u043D\u0435\u043D\u0438\u044F\u0020\u0441\u0020\u0441\u0435\u0440\u0432\u0435\u0440\u043E\u043C"
        private const val INVALID_CREDENTIALS_MESSAGE = "\u041D\u0435\u0432\u0435\u0440\u043D\u044B\u0439\u0020\u0075\u0073\u0065\u0072\u006E\u0061\u006D\u0065\u0020\u0438\u043B\u0438\u0020\u043F\u0430\u0440\u043E\u043B\u044C"
        private const val PASSWORD_TOO_SHORT = "\u041F\u0430\u0440\u043E\u043B\u044C\u0020\u0434\u043E\u043B\u0436\u0435\u043D\u0020\u0441\u043E\u0434\u0435\u0440\u0436\u0430\u0442\u044C\u0020\u043C\u0438\u043D\u0438\u043C\u0443\u043C\u0020\u0038\u0020\u0441\u0438\u043C\u0432\u043E\u043B\u043E\u0432"
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submitToken_validFormat() = runTest {
        val deps = FakeConnectDependencies().apply {
            connectResult = ApiResult.Success(
                ConnectResponse(
                    sessionToken = "session-1",
                    server = serverDto(),
                    status = "needs_profile",
                ),
            )
        }
        val viewModel = ConnectViewModel(deps)

        viewModel.connectWithToken("server.example.com:3000/ABCD1234")
        advanceUntilIdle()

        val state = viewModel.state.value as ConnectUiState.AuthChoice
        assertEquals("http://server.example.com:3000", state.serverAddress)
        assertEquals("Test Server", state.serverName)
        assertEquals("ABCD1234", state.tokenCode)
    }

    @Test
    fun submitToken_emptyInput() {
        val viewModel = ConnectViewModel(FakeConnectDependencies())

        viewModel.connectWithToken("")

        assertEquals(
            INVALID_TOKEN_FORMAT,
            (viewModel.state.value as ConnectUiState.Error).message,
        )
    }

    @Test
    fun submitToken_networkError() = runTest {
        val deps = FakeConnectDependencies().apply {
            connectResult = ApiResult.Failure(ApiError.NetworkError)
        }
        val viewModel = ConnectViewModel(deps)

        viewModel.connectWithToken("server.example.com:3000/ABCD1234")
        advanceUntilIdle()

        assertEquals(
            NETWORK_ERROR_MESSAGE,
            (viewModel.state.value as ConnectUiState.Error).message,
        )
    }

    @Test
    fun createProfile_pendingApproval() = runTest {
        val deps = FakeConnectDependencies().apply {
            connectResult = ApiResult.Success(
                ConnectResponse(
                    sessionToken = "session-1",
                    server = serverDto(),
                    status = "needs_profile",
                ),
            )
            createUserResult = ApiResult.Success(
                CreateUserResult.Pending(
                    AuthResponse(
                        sessionToken = "session-2",
                        user = userDto(username = "@alex"),
                        server = serverDto(),
                        status = "pending",
                    ),
                ),
            )
        }
        val viewModel = ConnectViewModel(deps)
        viewModel.connectWithToken("server.example.com:3000/ABCD1234")
        advanceUntilIdle()

        viewModel.createProfile(username = "alex", name = "Alex", password = "password123")
        advanceUntilIdle()

        val state = viewModel.state.value as ConnectUiState.Pending
        assertEquals("http://server.example.com:3000", state.serverAddress)
        assertEquals("Test Server", state.serverName)
        assertEquals("@alex", state.userName)
        assertEquals(1, deps.savedPendingApprovals.size)
    }

    @Test
    fun createProfile_immediateJoin() = runTest {
        val deps = FakeConnectDependencies().apply {
            connectResult = ApiResult.Success(
                ConnectResponse(
                    sessionToken = "session-1",
                    server = serverDto(),
                    status = "needs_profile",
                ),
            )
            createUserResult = ApiResult.Success(
                CreateUserResult.Joined(
                    User(
                        id = "user-1",
                        name = "Alex",
                        username = "@alex",
                    ),
                ),
            )
            loginResult = ApiResult.Success(
                AuthResponse(
                    sessionToken = "session-joined",
                    user = userDto(id = "user-1", username = "@alex"),
                    server = serverDto(),
                    status = "joined",
                ),
            )
        }
        val viewModel = ConnectViewModel(deps)
        viewModel.connectWithToken("server.example.com:3000/ABCD1234")
        advanceUntilIdle()

        viewModel.createProfile(username = "alex", name = "Alex", password = "password123")
        advanceUntilIdle()

        val state = viewModel.state.value as ConnectUiState.Joined
        assertEquals("http://server.example.com:3000", state.serverAddress)
        assertEquals("Test Server", state.serverName)
        assertEquals(1, deps.savedSessions.size)
        assertEquals("session-joined", deps.restoredSessions.single().second)
    }

    @Test
    fun login_success() = runTest {
        val deps = FakeConnectDependencies().apply {
            connectResult = ApiResult.Success(
                ConnectResponse(
                    sessionToken = "session-1",
                    server = serverDto(),
                    status = "needs_profile",
                ),
            )
            loginResult = ApiResult.Success(
                AuthResponse(
                    sessionToken = "session-login",
                    user = userDto(id = "user-42", username = "@alex"),
                    server = serverDto(),
                    status = "joined",
                ),
            )
        }
        val viewModel = ConnectViewModel(deps)
        viewModel.connectWithToken("server.example.com:3000/ABCD1234")
        advanceUntilIdle()

        viewModel.login(username = "alex", password = "password123")
        advanceUntilIdle()

        val state = viewModel.state.value as ConnectUiState.Joined
        assertEquals("http://server.example.com:3000", state.serverAddress)
        assertEquals("Test Server", state.serverName)
        assertEquals(1, deps.savedSessions.size)
    }

    @Test
    fun login_invalidCredentials() = runTest {
        val deps = FakeConnectDependencies().apply {
            connectResult = ApiResult.Success(
                ConnectResponse(
                    sessionToken = "session-1",
                    server = serverDto(),
                    status = "needs_profile",
                ),
            )
            loginResult = ApiResult.Failure(ApiError.Unauthorized())
        }
        val viewModel = ConnectViewModel(deps)
        viewModel.connectWithToken("server.example.com:3000/ABCD1234")
        advanceUntilIdle()

        viewModel.login(username = "alex", password = "wrongpass")
        advanceUntilIdle()

        assertEquals(
            INVALID_CREDENTIALS_MESSAGE,
            (viewModel.state.value as ConnectUiState.LoginError).message,
        )
    }

    @Test
    fun createProfile_validatesPasswordLength() = runTest {
        val deps = FakeConnectDependencies().apply {
            connectResult = ApiResult.Success(
                ConnectResponse(
                    sessionToken = "session-1",
                    server = serverDto(),
                    status = "needs_profile",
                ),
            )
        }
        val viewModel = ConnectViewModel(deps)
        viewModel.connectWithToken("server.example.com:3000/ABCD1234")
        advanceUntilIdle()

        viewModel.createProfile(username = "alex", name = "Alex", password = "short")

        assertEquals(
            PASSWORD_TOO_SHORT,
            (viewModel.state.value as ConnectUiState.Error).message,
        )
        assertTrue(deps.createUserCalls.isEmpty())
    }

    private class FakeConnectDependencies : ConnectDependencies {
        var connectResult: ApiResult<ConnectResponse> = ApiResult.Failure(ApiError.NetworkError)
        var createUserResult: ApiResult<CreateUserResult> = ApiResult.Failure(ApiError.NetworkError)
        var loginResult: ApiResult<AuthResponse> = ApiResult.Failure(ApiError.NetworkError)

        val createUserCalls = mutableListOf<CreateUserCall>()
        val savedPendingApprovals = mutableListOf<SavedPendingApproval>()
        val savedSessions = mutableListOf<SavedSession>()
        val restoredSessions = mutableListOf<Pair<String, String>>()

        override fun parseInviteToken(rawToken: String): Pair<String, String>? =
            com.callapp.android.network.ServerConnectionManager.parseInviteToken(rawToken)

        override suspend fun connect(serverAddress: String, inviteToken: String): ApiResult<ConnectResponse> =
            connectResult

        override suspend fun createUser(
            serverAddress: String,
            name: String,
            username: String,
            password: String,
        ): ApiResult<CreateUserResult> {
            createUserCalls += CreateUserCall(serverAddress, name, username, password)
            return createUserResult
        }

        override suspend fun login(
            serverAddress: String,
            inviteToken: String,
            username: String,
            password: String,
        ): ApiResult<AuthResponse> = loginResult

        override fun restoreSession(serverAddress: String, sessionToken: String) {
            restoredSessions += serverAddress to sessionToken
        }

        override fun saveSession(
            serverAddress: String,
            sessionToken: String,
            userId: String,
            server: Server?,
        ) {
            savedSessions += SavedSession(serverAddress, sessionToken, userId, server)
        }

        override fun savePendingApproval(
            serverAddress: String,
            inviteToken: String,
            username: String,
            password: String,
            serverName: String,
        ) {
            savedPendingApprovals += SavedPendingApproval(serverAddress, inviteToken, username, password, serverName)
        }

        override fun removePendingApproval(serverAddress: String) = Unit
    }

    private data class CreateUserCall(
        val serverAddress: String,
        val name: String,
        val username: String,
        val password: String,
    )

    private data class SavedPendingApproval(
        val serverAddress: String,
        val inviteToken: String,
        val username: String,
        val password: String,
        val serverName: String,
    )

    private data class SavedSession(
        val serverAddress: String,
        val sessionToken: String,
        val userId: String,
        val server: Server?,
    )

    private fun serverDto() = ServerDto(
        id = "srv-1",
        name = "Test Server",
        username = "@test",
    )

    private fun userDto(
        id: String = "user-1",
        username: String = "@alex",
    ) = UserDto(
        id = id,
        username = username,
        displayName = "Alex",
        serverId = "srv-1",
    )
}
