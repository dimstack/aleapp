package com.callapp.android.ui.screens.notifications

import androidx.lifecycle.SavedStateHandle
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.NotificationType
import com.callapp.android.domain.model.Server
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

        assertEquals(notifications, viewModel.state.value.notifications)
        assertEquals(1, viewModel.unreadCount.value)
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
        assertEquals(0, viewModel.unreadCount.value)
        assertEquals(1, dependencies.markReadCalls)
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
        assertEquals(0, viewModel.unreadCount.value)
        assertEquals(1, dependencies.clearCalls)
    }

    @Test
    fun unreadCount_calculatesCorrectly() = runTest {
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

        assertEquals(2, viewModel.unreadCount.value)

        viewModel.markAsRead("n1")
        advanceUntilIdle()

        assertEquals(1, viewModel.unreadCount.value)
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
        assertEquals(0, viewModel.unreadCount.value)
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

        assertEquals(notifications, viewModel.state.value.notifications)
        assertTrue(viewModel.state.value.error != null)
        assertFalse(viewModel.state.value.notifications.isEmpty())
    }

    @Test
    fun markAsRead_marksSingleNotification() = runTest {
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

        viewModel.markAsRead("n2")
        advanceUntilIdle()

        assertEquals(listOf(false, true), viewModel.state.value.notifications.map { it.isRead })
        assertEquals(1, viewModel.unreadCount.value)
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
        var markNotificationsReadResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var clearNotificationsResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var markReadCalls = 0
        var clearCalls = 0

        override fun getServerById(serverId: String): Server = server

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
