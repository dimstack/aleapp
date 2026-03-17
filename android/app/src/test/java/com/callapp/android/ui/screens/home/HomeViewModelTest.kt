package com.callapp.android.ui.screens.home

import app.cash.turbine.test
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.NotificationType
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.domain.model.User
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.common.UiState
import com.callapp.android.ui.screens.connect.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsServersAndFavorites() = runTest {
        val server = testServer()
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
            connectedServers = listOf(server),
            activeServerAddress = server.address,
        ).apply {
            favoritesResult = ApiResult.Success(listOf(testUser(serverId = server.id)))
        }

        val viewModel = HomeViewModel(dependencies)

        viewModel.serversState.test {
            assertEquals(UiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(UiState.Success(listOf(server)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(
            UiState.Success(listOf(testUser(serverId = server.id).toFavoriteContactItem(server))),
            viewModel.favoritesState.value,
        )
        assertEquals(listOf(server.address), dependencies.getFavoritesCalls)
    }

    @Test
    fun init_serverUnavailable() = runTest {
        val unavailableServer = testServer(
            availabilityStatus = ServerAvailabilityStatus.UNAVAILABLE,
            availabilityMessage = "Сервер недоступен",
        )
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(unavailableServer)),
            connectedServers = listOf(unavailableServer),
            activeServerAddress = unavailableServer.address,
        )

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        val state = viewModel.serversState.value as UiState.Success
        assertEquals(ServerAvailabilityStatus.UNAVAILABLE, state.data.single().availabilityStatus)
    }

    @Test
    fun init_networkError() = runTest {
        val dependencies = FakeHomeDependencies(
            serversFlow = flow { throw IllegalStateException("boom") },
        )

        val viewModel = HomeViewModel(dependencies)

        viewModel.serversState.test {
            assertEquals(UiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(UiState.Error("Не удалось загрузить серверы"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun init_callsProcessPendingApprovals() = runTest {
        val dependencies = FakeHomeDependencies()

        HomeViewModel(dependencies)
        advanceUntilIdle()

        assertEquals(1, dependencies.processPendingApprovalsCalls)
    }

    @Test
    fun init_withoutConnectedServers_setsEmptyFavoritesAndZeroNotifications() = runTest {
        val dependencies = FakeHomeDependencies(activeServerAddress = "")

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        assertEquals(UiState.Success(emptyList<FavoriteContactItem>()), viewModel.favoritesState.value)
        assertEquals(0, viewModel.notificationCount.value)
        assertTrue(dependencies.getFavoritesCalls.isEmpty())
        assertEquals(0, dependencies.getNotificationsCalls)
    }

    @Test
    fun init_favoritesError_setsErrorState() = runTest {
        val server = testServer()
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
            connectedServers = listOf(server),
            activeServerAddress = server.address,
        ).apply {
            favoritesResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.favoritesState.value is UiState.Error)
    }

    @Test
    fun init_notificationsUnauthorized_setsFavoritesErrorAndResetsCount() = runTest {
        val server = testServer()
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
            connectedServers = listOf(server),
            activeServerAddress = server.address,
        ).apply {
            favoritesResult = ApiResult.Success(listOf(testUser(serverId = server.id)))
            notificationsResult = ApiResult.Failure(ApiError.Unauthorized(code = "unauthorized"))
        }

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.favoritesState.value is UiState.Error)
        assertEquals(0, viewModel.notificationCount.value)
    }

    @Test
    fun notificationCount_updatesWhenNotificationsChange() = runTest {
        val server = testServer()
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
            connectedServers = listOf(server),
            activeServerAddress = server.address,
        ).apply {
            notificationsResult = ApiResult.Success(
                listOf(
                    testNotification(id = "n1", isRead = false),
                    testNotification(id = "n2", isRead = false),
                    testNotification(id = "n3", isRead = true),
                ),
            )
        }

        val viewModel = HomeViewModel(dependencies)

        viewModel.notificationCount.test {
            assertEquals(0, awaitItem())
            advanceUntilIdle()
            assertEquals(2, awaitItem())

            dependencies.notificationsResult = ApiResult.Success(
                listOf(
                    testNotification(id = "n1", isRead = false),
                    testNotification(id = "n2", isRead = true),
                ),
            )
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_reloadsAllData() = runTest {
        val server = testServer()
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
            connectedServers = listOf(server),
            activeServerAddress = server.address,
        )

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, dependencies.processPendingApprovalsCalls)
        assertEquals(2, dependencies.refreshConnectedServersAvailabilityCalls)
        assertEquals(2, dependencies.getFavoritesCalls.size)
        assertEquals(2, dependencies.getNotificationsCalls)
    }

    @Test
    fun favoriteUpdate_refreshesFavoritesImmediately() = runTest {
        val server = testServer()
        val favoriteUpdates = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
            connectedServers = listOf(server),
            favoriteUpdates = favoriteUpdates,
            activeServerAddress = server.address,
        ).apply {
            favoritesResult = ApiResult.Success(emptyList())
        }

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        dependencies.favoritesResult = ApiResult.Success(listOf(testUser(serverId = server.id)))
        favoriteUpdates.tryEmit(server.address)
        advanceUntilIdle()

        assertEquals(2, dependencies.getFavoritesCalls.size)
        assertEquals(
            UiState.Success(listOf(testUser(serverId = server.id).toFavoriteContactItem(server))),
            viewModel.favoritesState.value,
        )
    }

    @Test
    fun userUpdate_refreshesFavoritesImmediately() = runTest {
        val server = testServer()
        val userUpdates = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val avatarUrl = "https://server.example.com/uploads/profile/new.jpg"
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
            connectedServers = listOf(server),
            userUpdates = userUpdates,
            activeServerAddress = server.address,
        ).apply {
            favoritesResult = ApiResult.Success(emptyList())
        }

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        dependencies.favoritesResult = ApiResult.Success(
            listOf(testUser(serverId = server.id, avatarUrl = avatarUrl)),
        )
        userUpdates.tryEmit(server.address)
        advanceUntilIdle()

        assertEquals(2, dependencies.getFavoritesCalls.size)
        val favorites = (viewModel.favoritesState.value as UiState.Success).data
        assertEquals(avatarUrl, favorites.single().user.avatarUrl)
    }

    @Test
    fun refresh_keepsContentVisibleWhileReloading() = runTest {
        val server = testServer()
        val gate = CompletableDeferred<Unit>()
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
            connectedServers = listOf(server),
            activeServerAddress = server.address,
        ).apply {
            favoritesResult = ApiResult.Success(listOf(testUser(serverId = server.id)))
        }

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        dependencies.favoritesGate = gate
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            UiState.Success(listOf(testUser(serverId = server.id).toFavoriteContactItem(server))),
            viewModel.favoritesState.value,
        )
        assertTrue(viewModel.isRefreshing.value)

        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.favoritesState.value is UiState.Success)
        assertEquals(false, viewModel.isRefreshing.value)
    }

    @Test
    fun loadData_keepsCachedFavoritesForUnavailableServer() = runTest {
        val availableServer = testServer(id = "srv-1", address = "https://server-1.example")
        val unavailableServer = testServer(
            id = "srv-2",
            address = "https://server-2.example",
            availabilityStatus = ServerAvailabilityStatus.UNAVAILABLE,
            availabilityMessage = "Сервер недоступен",
        )
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(availableServer, unavailableServer)),
            connectedServers = listOf(availableServer, unavailableServer),
            activeServerAddress = availableServer.address,
        ).apply {
            favoritesByAddress[availableServer.address] =
                ApiResult.Success(listOf(testUser(id = "user-1", serverId = availableServer.id, username = "@alex")))
            favoritesByAddress[unavailableServer.address] =
                ApiResult.Success(listOf(testUser(id = "user-2", serverId = unavailableServer.id, name = "Maria", username = "@maria")))
        }

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        dependencies.favoritesByAddress[unavailableServer.address] = ApiResult.Failure(ApiError.NetworkError)
        viewModel.refresh()
        advanceUntilIdle()

        val favorites = (viewModel.favoritesState.value as UiState.Success).data
        assertEquals(2, favorites.size)
        val unavailableFavorite = favorites.single { it.user.serverId == unavailableServer.id }
        assertTrue(unavailableFavorite.isUnavailable)
        assertEquals("Сервер недоступен", unavailableFavorite.serverAvailabilityMessage)
    }

    private class FakeHomeDependencies(
        private val serversFlow: Flow<List<Server>> = MutableStateFlow(emptyList()),
        private val connectedServers: List<Server> = emptyList(),
        private val favoriteUpdates: Flow<String> = MutableSharedFlow(),
        private val userUpdates: Flow<String> = MutableSharedFlow(),
        private val activeServerAddress: String = "",
    ) : HomeDependencies {
        var favoritesResult: ApiResult<List<User>> = ApiResult.Success(emptyList())
        val favoritesByAddress = linkedMapOf<String, ApiResult<List<User>>>()
        var notificationsResult: ApiResult<List<Notification>> = ApiResult.Success(emptyList())

        var processPendingApprovalsCalls = 0
        var refreshConnectedServersAvailabilityCalls = 0
        val getFavoritesCalls = mutableListOf<String>()
        var getNotificationsCalls = 0
        var favoritesGate: CompletableDeferred<Unit>? = null

        override fun observeConnectedServers(): Flow<List<Server>> = serversFlow

        override fun connectedServers(): List<Server> = connectedServers

        override fun observeFavoriteUpdates(): Flow<String> = favoriteUpdates

        override fun observeUserUpdates(): Flow<String> = userUpdates

        override suspend fun processPendingApprovals() {
            processPendingApprovalsCalls += 1
        }

        override suspend fun refreshConnectedServersAvailability() {
            refreshConnectedServersAvailabilityCalls += 1
        }

        override suspend fun getFavorites(serverAddress: String): ApiResult<List<User>> {
            getFavoritesCalls += serverAddress
            favoritesGate?.await()
            return favoritesByAddress[serverAddress] ?: favoritesResult
        }

        override suspend fun getNotifications(serverAddress: String): ApiResult<List<Notification>> {
            getNotificationsCalls += 1
            return notificationsResult
        }

        override fun activeServerAddress(): String = activeServerAddress
    }

    private fun testServer(
        id: String = "srv-1",
        address: String = "https://server.example.com",
        availabilityStatus: ServerAvailabilityStatus = ServerAvailabilityStatus.AVAILABLE,
        availabilityMessage: String? = null,
    ) = Server(
        id = id,
        name = "Test Server",
        username = "@test",
        address = address,
        availabilityStatus = availabilityStatus,
        availabilityMessage = availabilityMessage,
    )

    private fun testUser(
        id: String = "user-1",
        serverId: String = "srv-1",
        name: String = "Alex",
        username: String = "@alex",
        avatarUrl: String? = null,
    ) = User(
        id = id,
        name = name,
        username = username,
        avatarUrl = avatarUrl,
        serverId = serverId,
    )

    private fun testNotification(
        id: String,
        isRead: Boolean,
    ) = Notification(
        id = id,
        type = NotificationType.REQUEST_SENT,
        serverName = "Test Server",
        message = "Test notification",
        isRead = isRead,
        createdAt = "2026-03-15T10:00:00Z",
    )
}
