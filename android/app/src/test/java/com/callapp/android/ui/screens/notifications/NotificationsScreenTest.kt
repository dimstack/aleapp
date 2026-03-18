package com.callapp.android.ui.screens.notifications

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.NotificationType
import com.callapp.android.domain.model.Server
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val server = Server(
        id = "server-1",
        name = "Горилла",
        username = "@gorilla",
    )

    private val unreadNotification = Notification(
        id = "notif-1",
        type = NotificationType.MISSED_CALL,
        serverName = "Горилла",
        message = "Алексей пытался дозвониться вам, но вызов остался без ответа",
        actorUserId = "user-1",
        actorUsername = "@makak",
        actorDisplayName = "Алексей Миронов",
        isRead = false,
        createdAt = "2026-03-15T10:00:00Z",
    )

    @Test
    fun showsMissedCallContentInFavoritesStyle() {
        composeRule.setAleAppContent {
            NotificationsScreen(
                notifications = listOf(unreadNotification),
                server = server,
            )
        }

        composeRule.onNodeWithText("Пропущенный вызов").assertIsDisplayed()
        composeRule.onNodeWithText("Алексей Миронов").assertIsDisplayed()
        composeRule.onNodeWithText("@makak", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("gorilla", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("13:00").assertIsDisplayed()
        composeRule.onNodeWithText("15 марта").assertIsDisplayed()
    }

    @Test
    fun groupsMissedCallsFromSameUserIntoSingleCard() {
        val groupedNotifications = listOf(
            unreadNotification.copy(id = "notif-1", createdAt = "2026-03-15T10:00:00Z"),
            unreadNotification.copy(id = "notif-2", createdAt = "2026-03-15T09:40:00Z"),
        )

        composeRule.setAleAppContent {
            NotificationsScreen(
                notifications = groupedNotifications,
                server = server,
            )
        }

        composeRule.onNodeWithText("Пропущено вызовов: 2").assertIsDisplayed()
        composeRule.onAllNodesWithText("Алексей Миронов").assertCountEquals(1)
    }

    @Test
    fun clearAllTriggersCallback() {
        var cleared = false

        composeRule.setAleAppContent {
            NotificationsScreen(
                notifications = listOf(unreadNotification),
                server = server,
                onClearAll = { cleared = true },
            )
        }

        composeRule.onNodeWithText("Очистить все").performClick()

        assertEquals(true, cleared)
    }

    @Test
    fun emptyStateIsShownForEmptyList() {
        composeRule.setAleAppContent {
            NotificationsScreen(notifications = emptyList())
        }

        composeRule.onNodeWithText("Уведомлений нет").assertIsDisplayed()
    }
}
