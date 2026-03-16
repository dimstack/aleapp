package com.callapp.android.ui.screens.call

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IncomingCallScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCallerName() {
        composeRule.setAleAppContent {
            IncomingCallScreen(
                contactName = "Анна Смирнова",
                serverName = "Tech Community",
            )
        }

        composeRule.onNodeWithText("Анна Смирнова").assertIsDisplayed()
    }

    @Test
    fun acceptButton_triggersAccept() {
        var accepted = false

        composeRule.setAleAppContent {
            IncomingCallScreen(
                contactName = "Анна Смирнова",
                onAccept = { accepted = true },
            )
        }

        composeRule.onNodeWithTag("incoming_call_accept_button").performClick()

        assertTrue(accepted)
    }

    @Test
    fun declineButton_triggersDecline() {
        var declined = false

        composeRule.setAleAppContent {
            IncomingCallScreen(
                contactName = "Анна Смирнова",
                onDecline = { declined = true },
            )
        }

        composeRule.onNodeWithTag("incoming_call_decline_button").performClick()

        assertTrue(declined)
    }

    @Test
    fun showsCorrectCallType() {
        composeRule.setAleAppContent {
            IncomingCallScreen(
                contactName = "Анна Смирнова",
                callType = CallType.VIDEO,
            )
        }

        composeRule.onAllNodesWithTag("incoming_call_type_marker_video", useUnmergedTree = true)
            .assertCountEquals(1)
    }
}
