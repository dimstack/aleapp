package com.callapp.android.ui.screens.connect

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PendingRequestScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pendingStateShowsHelperText() {
        composeRule.setAleAppContent {
            PendingRequestScreen(
                serverName = "Tech Community",
                userName = "@anna",
                status = RequestStatus.PENDING,
            )
        }

        composeRule.onNodeWithText("Заявка отправлена").assertIsDisplayed()
        composeRule.onNodeWithText("@anna").assertIsDisplayed()
    }
}
