package com.callapp.android.ui.screens.notifications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.callapp.android.domain.model.Notification
import com.callapp.android.domain.model.NotificationType
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

    private val unreadNotification = Notification(
        id = "notif-1",
        type = NotificationType.REQUEST_APPROVED,
        serverName = "Tech Community",
        message = "Заявка одобрена",
        isRead = false,
        createdAt = "15 марта, 10:00",
    )

    @Test
    fun showsUnreadCounterAndNotificationContent() {
        composeRule.setAleAppContent {
            NotificationsScreen(
                notifications = listOf(unreadNotification),
            )
        }

        composeRule.onNodeWithText("1 непрочитанных").assertIsDisplayed()
        composeRule.onNodeWithText("Tech Community").assertIsDisplayed()
        composeRule.onNodeWithText("15 марта, 10:00").assertIsDisplayed()
    }

    @Test
    fun clearAllTriggersCallback() {
        var cleared = false

        composeRule.setAleAppContent {
            NotificationsScreen(
                notifications = listOf(unreadNotification),
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
