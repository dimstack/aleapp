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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            activeServerAddress = server.address,
        ).apply {
            favoritesResult = ApiResult.Success(listOf(testUser()))
        }

        val viewModel = HomeViewModel(dependencies)

        viewModel.serversState.test {
            assertEquals(UiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(UiState.Success(listOf(server)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(viewModel.favoritesState.value is UiState.Success)
        assertEquals(1, dependencies.getFavoritesCalls)
    }

    @Test
    fun init_serverUnavailable() = runTest {
        val unavailableServer = testServer(
            availabilityStatus = ServerAvailabilityStatus.UNAVAILABLE,
            availabilityMessage = "Сервер недоступен",
        )
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(unavailableServer)),
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
    fun init_withoutActiveServer_setsEmptyFavoritesAndZeroNotifications() = runTest {
        val server = testServer()
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
            activeServerAddress = "",
        )

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        assertEquals(UiState.Success(emptyList<User>()), viewModel.favoritesState.value)
        assertEquals(0, viewModel.notificationCount.value)
        assertEquals(0, dependencies.getFavoritesCalls)
        assertEquals(0, dependencies.getNotificationsCalls)
    }

    @Test
    fun init_favoritesError_setsErrorState() = runTest {
        val server = testServer()
        val dependencies = FakeHomeDependencies(
            serversFlow = MutableStateFlow(listOf(server)),
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
            activeServerAddress = server.address,
        ).apply {
            favoritesResult = ApiResult.Success(listOf(testUser()))
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
            activeServerAddress = server.address,
        )

        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, dependencies.processPendingApprovalsCalls)
        assertEquals(2, dependencies.refreshConnectedServersAvailabilityCalls)
        assertEquals(2, dependencies.getFavoritesCalls)
        assertEquals(2, dependencies.getNotificationsCalls)
    }

    private class FakeHomeDependencies(
        private val serversFlow: Flow<List<Server>> = MutableStateFlow(emptyList()),
        private val activeServerAddress: String = "",
    ) : HomeDependencies {
        var favoritesResult: ApiResult<List<User>> = ApiResult.Success(emptyList())
        var notificationsResult: ApiResult<List<Notification>> = ApiResult.Success(emptyList())

        var processPendingApprovalsCalls = 0
        var refreshConnectedServersAvailabilityCalls = 0
        var getFavoritesCalls = 0
        var getNotificationsCalls = 0

        override fun observeConnectedServers(): Flow<List<Server>> = serversFlow

        override suspend fun processPendingApprovals() {
            processPendingApprovalsCalls += 1
        }

        override suspend fun refreshConnectedServersAvailability() {
            refreshConnectedServersAvailabilityCalls += 1
        }

        override suspend fun getFavorites(serverAddress: String): ApiResult<List<User>> {
            getFavoritesCalls += 1
            return favoritesResult
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

    private fun testUser() = User(
        id = "user-1",
        name = "Alex",
        username = "@alex",
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
