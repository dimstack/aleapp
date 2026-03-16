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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

        var usersResult: ApiResult<List<User>> = ApiResult.Success(emptyList())
        var joinRequestsResult: ApiResult<List<JoinRequest>> = ApiResult.Success(emptyList())
        var removeUserResult: ApiResult<Unit> = ApiResult.Success(Unit)
        val removeUserCalls = mutableListOf<String>()

        override fun getServerById(serverId: String): Server = serverFlow.value

        override fun observeServerById(serverId: String): Flow<Server?> = flowOf(serverFlow.value)

        override suspend fun getUsers(serverAddress: String): ApiResult<List<User>> = usersResult

        override suspend fun getJoinRequests(serverAddress: String): ApiResult<List<JoinRequest>> =
            joinRequestsResult

        override suspend fun removeUser(serverAddress: String, userId: String): ApiResult<Unit> {
            removeUserCalls += userId
            return removeUserResult
        }

        override fun disconnectServer(serverAddress: String) = Unit

        override fun currentUserId(): String = currentUserId
    }

    private companion object {
        fun testServer(
            id: String = "srv-1",
            address: String = "https://server.example.com",
        ) = Server(
            id = id,
            name = "Test Server",
            username = "@test",
            address = address,
        )

        fun testUser(
            id: String,
            name: String,
            role: UserRole = UserRole.MEMBER,
        ) = User(
            id = id,
            name = name,
            username = "@${name.lowercase()}",
            role = role,
            serverId = "srv-1",
        )
    }
}
