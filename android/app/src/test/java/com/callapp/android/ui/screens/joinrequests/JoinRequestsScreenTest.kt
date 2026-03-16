package com.callapp.android.ui.screens.joinrequests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JoinRequestsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val requests = listOf(
        JoinRequestItem("req-1", "Анна Смирнова", "@anna", "15 марта"),
        JoinRequestItem("req-2", "Мария Иванова", "@maria", "16 марта"),
    )

    @Test
    fun showsRequestsAndCount() {
        composeRule.setAleAppContent {
            JoinRequestsScreen(
                serverName = "Tech Community",
                requests = requests,
            )
        }

        composeRule.onNodeWithText("Новые заявки").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("Анна Смирнова").assertIsDisplayed()
        composeRule.onNodeWithText("Мария Иванова").assertIsDisplayed()
    }

    @Test
    fun approveAndDeclineCallbacksReceiveCorrectIds() {
        val actions = mutableListOf<String>()

        composeRule.setAleAppContent {
            JoinRequestsScreen(
                serverName = "Tech Community",
                requests = requests,
                onApprove = { actions += "approve:$it" },
                onDecline = { actions += "decline:$it" },
            )
        }

        composeRule.onAllNodesWithText("Принять")[0].performClick()
        composeRule.onAllNodesWithText("Отклонить")[1].performClick()

        assertEquals(listOf("approve:req-1", "decline:req-2"), actions)
    }

    @Test
    fun emptyStateIsShown() {
        composeRule.setAleAppContent {
            JoinRequestsScreen(
                serverName = "Tech Community",
                requests = emptyList(),
            )
        }

        composeRule.onNodeWithText("Новых заявок нет").assertIsDisplayed()
    }
}
