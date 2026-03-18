package com.callapp.android.ui.screens.notifications

import androidx.lifecycle.SavedStateHandle
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.NotificationType
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
import com.callapp.android.ui.screens.connect.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadNotifications_success() = runTest {
        val notifications = listOf(
            testNotification(id = "n1", isRead = false),
            testNotification(id = "n2", isRead = true),
        )
        val dependencies = FakeNotificationsDependencies().apply {
            notificationsResult = ApiResult.Success(notifications)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.notifications.all { it.isRead })
        assertEquals(1, dependencies.markReadCalls)
    }

    @Test
    fun markAllRead() = runTest {
        val dependencies = FakeNotificationsDependencies().apply {
            notificationsResult = ApiResult.Success(
                listOf(
                    testNotification(id = "n1", isRead = false),
                    testNotification(id = "n2", isRead = false),
                ),
            )
        }
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.markAllRead()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.notifications.all { it.isRead })
        assertEquals(2, dependencies.markReadCalls)
    }

    @Test
    fun clearAll() = runTest {
        val dependencies = FakeNotificationsDependencies().apply {
            notificationsResult = ApiResult.Success(
                listOf(
                    testNotification(id = "n1", isRead = false),
                    testNotification(id = "n2", isRead = true),
                ),
            )
        }
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.clearAll()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.notifications.isEmpty())
        assertEquals(1, dependencies.clearCalls)
    }

    @Test
    fun loadNotifications_marksEverythingReadImmediately() = runTest {
        val dependencies = FakeNotificationsDependencies().apply {
            notificationsResult = ApiResult.Success(
                listOf(
                    testNotification(id = "n1", isRead = false),
                    testNotification(id = "n2", isRead = false),
                    testNotification(id = "n3", isRead = true),
                ),
            )
        }
        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.notifications.all { it.isRead })
        assertEquals(1, dependencies.markReadCalls)
    }

    @Test
    fun loadNotifications_networkError() = runTest {
        val dependencies = FakeNotificationsDependencies().apply {
            notificationsResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error != null)
        assertTrue(viewModel.state.value.notifications.isEmpty())
    }

    @Test
    fun clearAll_networkError_keepsList() = runTest {
        val notifications = listOf(
            testNotification(id = "n1", isRead = false),
            testNotification(id = "n2", isRead = true),
        )
        val dependencies = FakeNotificationsDependencies().apply {
            notificationsResult = ApiResult.Success(notifications)
            clearNotificationsResult = ApiResult.Failure(ApiError.NetworkError)
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        viewModel.clearAll()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.notifications.all { it.isRead })
        assertTrue(viewModel.state.value.error != null)
        assertFalse(viewModel.state.value.notifications.isEmpty())
    }

    @Test
    fun loadNotifications_restoresUsernameFromServerUsers() = runTest {
        val dependencies = FakeNotificationsDependencies().apply {
            notificationsResult = ApiResult.Success(
                listOf(
                    Notification(
                        id = "missed-1",
                        type = NotificationType.MISSED_CALL,
                        serverName = "Test Server",
                        message = "Missed call",
                        actorUserId = "user-1",
                        actorDisplayName = "Caller",
                        isRead = false,
                        createdAt = "2026-03-16T10:00:00Z",
                    ),
                ),
            )
            usersResult = ApiResult.Success(
                listOf(
                    User(
                        id = "user-1",
                        name = "Caller",
                        username = "@caller",
                        serverId = "srv-1",
                    ),
                ),
            )
        }

        val viewModel = createViewModel(dependencies)
        advanceUntilIdle()

        assertEquals("@caller", viewModel.state.value.notifications.single().actorUsername)
    }

    private fun createViewModel(
        dependencies: NotificationsDependencies,
    ) = NotificationsViewModel(
        savedStateHandle = SavedStateHandle(mapOf("serverId" to "srv-1")),
        dependencies = dependencies,
    )

    private class FakeNotificationsDependencies : NotificationsDependencies {
        var server = Server(
            id = "srv-1",
            name = "Test Server",
            username = "@test",
            address = "https://server.example.com",
        )
        var notificationsResult: ApiResult<List<Notification>> = ApiResult.Success(emptyList())
        var usersResult: ApiResult<List<User>> = ApiResult.Success(emptyList())
        var markNotificationsReadResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var clearNotificationsResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var markReadCalls = 0
        var clearCalls = 0

        override fun getServerById(serverId: String): Server = server

        override suspend fun getUsers(serverAddress: String): ApiResult<List<User>> = usersResult

        override suspend fun getNotifications(serverAddress: String): ApiResult<List<Notification>> =
            notificationsResult

        override suspend fun markNotificationsRead(serverAddress: String): ApiResult<Unit> {
            markReadCalls += 1
            return markNotificationsReadResult
        }

        override suspend fun clearNotifications(serverAddress: String): ApiResult<Unit> {
            clearCalls += 1
            return clearNotificationsResult
        }
    }

    private companion object {
        fun testNotification(
            id: String,
            isRead: Boolean,
        ) = Notification(
            id = id,
            type = NotificationType.REQUEST_SENT,
            serverName = "Test Server",
            message = "Notification $id",
            isRead = isRead,
            createdAt = "2026-03-16T10:00:00Z",
        )
    }
}
