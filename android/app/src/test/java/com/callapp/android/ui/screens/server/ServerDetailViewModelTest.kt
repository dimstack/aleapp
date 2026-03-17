package com.callapp.android.ui.screens.server

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserRole
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.UiState
import com.callapp.android.ui.screens.connect.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadUsers_success() = runTest {
        val server = testServer()
        val users = listOf(
            testUser(id = "admin-1", name = "Admin", role = UserRole.ADMIN),
            testUser(id = "user-1", name = "Alex"),
        )
        val dependencies = FakeServerDetailDependencies(server = server).apply {
            usersResult = ApiResult.Success(users)
        }

        val viewModel = createViewModel(dependencies, server.id)
        advanceUntilIdle()

        assertEquals(UiState.Success(users), viewModel.membersState.value)
        assertTrue(viewModel.isAdmin.value)
    }

    @Test
    fun loadUsers_networkError() = runTest {
        val dependencies = FakeServerDetailDependencies(server = testServer()).apply {
            usersResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        assertEquals(UiState.Error("Нет соединения с сервером"), viewModel.membersState.value)
    }

    @Test
    fun removeUser_success() = runTest {
        val server = testServer()
        val userToRemove = testUser(id = "user-1", name = "Alex")
        val dependencies = FakeServerDetailDependencies(server = server).apply {
            usersResult = ApiResult.Success(
                listOf(
                    testUser(id = "admin-1", name = "Admin", role = UserRole.ADMIN),
                    userToRemove,
                    testUser(id = "user-2", name = "Maria"),
                ),
            )
        }

        val viewModel = createViewModel(dependencies, server.id)
        advanceUntilIdle()

        viewModel.removeUser(userToRemove.id)
        advanceUntilIdle()

        val state = viewModel.membersState.value as UiState.Success
        assertEquals(listOf("admin-1", "user-2"), state.data.map { it.id })
        assertEquals(listOf(userToRemove.id), dependencies.removeUserCalls)
    }

    @Test
    fun removeUser_onlyAdmin() = runTest {
        val currentUser = testUser(id = "admin-1", name = "Admin", role = UserRole.ADMIN)
        val dependencies = FakeServerDetailDependencies(server = testServer()).apply {
            usersResult = ApiResult.Success(listOf(currentUser, testUser(id = "user-2", name = "Maria")))
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.removeUser(currentUser.id)
        advanceUntilIdle()

        assertEquals("Нельзя удалить себя", viewModel.actionError.value)
        assertTrue(dependencies.removeUserCalls.isEmpty())
    }

    @Test
    fun searchFilter() = runTest {
        val dependencies = FakeServerDetailDependencies(server = testServer()).apply {
            usersResult = ApiResult.Success(
                listOf(
                    testUser(id = "admin-1", name = "Admin", role = UserRole.ADMIN),
                    testUser(id = "user-1", name = "Alex"),
                    testUser(id = "user-2", name = "Maria"),
                ),
            )
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.updateSearchQuery("mar")
        advanceUntilIdle()

        assertEquals(listOf("user-2"), viewModel.filteredMembers.value.map { it.id })
    }

    @Test
    fun callUser_navigatesToCallScreen() = runTest {
        val server = testServer(address = "https://server.example.com")
        val dependencies = FakeServerDetailDependencies(server = server).apply {
            usersResult = ApiResult.Success(listOf(testUser(id = "admin-1", name = "Admin", role = UserRole.ADMIN)))
        }
        val viewModel = createViewModel(dependencies, server.id)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.callUser(userId = "user-42", contactName = "Maria")
            assertEquals(
                ServerDetailEvent.NavigateToCall(
                    serverAddress = server.address,
                    userId = "user-42",
                    contactName = "Maria",
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_keepsMembersVisibleWhileReloading() = runTest {
        val server = testServer()
        val gate = CompletableDeferred<Unit>()
        val users = listOf(
            testUser(id = "admin-1", name = "Admin", role = UserRole.ADMIN),
            testUser(id = "user-1", name = "Alex"),
        )
        val dependencies = FakeServerDetailDependencies(server = server).apply {
            usersResult = ApiResult.Success(users)
        }

        val viewModel = createViewModel(dependencies, server.id)
        advanceUntilIdle()

        dependencies.usersGate = gate
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(UiState.Success(users), viewModel.membersState.value)
        assertTrue(viewModel.isRefreshing.value)
        assertEquals(1, dependencies.refreshServerCalls)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(UiState.Success(users), viewModel.membersState.value)
        assertTrue(!viewModel.isRefreshing.value)
    }

    @Test
    fun refresh_updatesServerInfoFromRemote() = runTest {
        val server = testServer(name = "Old name", description = "Old description")
        val updatedServer = testServer(name = "New name", description = "New description")
        val dependencies = FakeServerDetailDependencies(server = server).apply {
            usersResult = ApiResult.Success(listOf(testUser(id = "admin-1", name = "Admin", role = UserRole.ADMIN)))
            refreshedServer = updatedServer
        }

        val viewModel = createViewModel(dependencies, server.id)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("New name", viewModel.server.value.name)
        assertEquals("New description", viewModel.server.value.description)
    }

    @Test
    fun invalidSessionDisconnectsServerAndEmitsEvent() = runTest {
        val server = testServer()
        val gate = CompletableDeferred<Unit>()
        val dependencies = FakeServerDetailDependencies(server = server).apply {
            usersGate = gate
            usersResult = ApiResult.Failure(ApiError.Unauthorized(message = "User session is invalid"))
        }

        val viewModel = createViewModel(dependencies, server.id)

        viewModel.events.test {
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(server.address), dependencies.disconnectedAddresses)
            assertEquals(ServerDetailEvent.ServerDisconnected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun userUpdate_refreshesMembers() = runTest {
        val server = testServer()
        val updatedUsers = listOf(
            testUser(
                id = "admin-1",
                name = "Admin",
                role = UserRole.ADMIN,
                avatarUrl = "https://server.example.com/uploads/profile/admin.jpg",
            ),
        )
        val dependencies = FakeServerDetailDependencies(server = server).apply {
            usersResult = ApiResult.Success(
                listOf(testUser(id = "admin-1", name = "Admin", role = UserRole.ADMIN)),
            )
        }

        val viewModel = createViewModel(dependencies, server.id)
        advanceUntilIdle()

        dependencies.usersResult = ApiResult.Success(updatedUsers)
        dependencies.userUpdates.tryEmit(server.address)
        advanceUntilIdle()

        val members = (viewModel.membersState.value as UiState.Success).data
        assertEquals("https://server.example.com/uploads/profile/admin.jpg", members.single().avatarUrl)
    }

    private fun createViewModel(
        dependencies: ServerDetailDependencies,
        serverId: String = "srv-1",
    ) = ServerDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("serverId" to serverId)),
        dependencies = dependencies,
    )

    private class FakeServerDetailDependencies(
        server: Server = testServer(),
        private val currentUserId: String = "admin-1",
    ) : ServerDetailDependencies {
        private val serverFlow = MutableStateFlow(server)
        val userUpdates = MutableSharedFlow<String>(extraBufferCapacity = 1)

        var usersResult: ApiResult<List<User>> = ApiResult.Success(emptyList())
        var joinRequestsResult: ApiResult<List<JoinRequest>> = ApiResult.Success(emptyList())
        var removeUserResult: ApiResult<Unit> = ApiResult.Success(Unit)
        val removeUserCalls = mutableListOf<String>()
        var usersGate: CompletableDeferred<Unit>? = null
        var refreshServerCalls = 0
        var refreshedServer: Server? = null
        val disconnectedAddresses = mutableListOf<String>()

        override fun getServerById(serverId: String): Server = serverFlow.value

        override fun observeServerById(serverId: String): Flow<Server?> = serverFlow

        override fun observeUserUpdates(): Flow<String> = userUpdates

        override suspend fun refreshServer(serverAddress: String) {
            refreshServerCalls += 1
            refreshedServer?.let { serverFlow.value = it }
        }

        override suspend fun getUsers(serverAddress: String): ApiResult<List<User>> {
            usersGate?.await()
            return usersResult
        }

        override suspend fun getJoinRequests(serverAddress: String): ApiResult<List<JoinRequest>> =
            joinRequestsResult

        override suspend fun removeUser(serverAddress: String, userId: String): ApiResult<Unit> {
            removeUserCalls += userId
            return removeUserResult
        }

        override fun disconnectServer(serverAddress: String) {
            disconnectedAddresses += serverAddress
        }

        override fun currentUserId(): String = currentUserId
    }

    private companion object {
        fun testServer(
            id: String = "srv-1",
            address: String = "https://server.example.com",
            name: String = "Test Server",
            description: String = "",
        ) = Server(
            id = id,
            name = name,
            username = "@test",
            description = description,
            address = address,
        )

        fun testUser(
            id: String,
            name: String,
            role: UserRole = UserRole.MEMBER,
            avatarUrl: String? = null,
        ) = User(
            id = id,
            name = name,
            username = "@${name.lowercase()}",
            avatarUrl = avatarUrl,
            role = role,
            serverId = "srv-1",
        )
    }
}
