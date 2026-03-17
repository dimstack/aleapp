package com.callapp.android.ui.screens.server

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UnavailableServerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersUnavailableServerMessage() {
        composeRule.setAleAppContent {
            UnavailableServerScreen(
                server = testServer(),
            )
        }

        composeRule.onNodeWithText("Offline Hub").assertIsDisplayed()
        composeRule.onNodeWithText("@offline").assertIsDisplayed()
        composeRule.onNodeWithText("Server is temporarily unavailable").assertIsDisplayed()
    }

    @Test
    fun retryAndRemove_invokeCallbacks() {
        var retried = false
        var removed = false

        composeRule.setAleAppContent {
            UnavailableServerScreen(
                server = testServer(),
                onRetry = { retried = true },
                onRemove = { removed = true },
            )
        }

        composeRule.onNodeWithText("Перепроверить подключение").performClick()
        composeRule.onNodeWithText("Удалить сервер").performClick()

        assertTrue(retried)
        assertTrue(removed)
    }

    private fun testServer() = Server(
        id = "srv-1",
        name = "Offline Hub",
        username = "@offline",
        address = "https://offline.example.com",
        availabilityStatus = ServerAvailabilityStatus.UNAVAILABLE,
        availabilityMessage = "Server is temporarily unavailable",
    )
}
