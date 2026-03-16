package com.callapp.android.ui.screens.connect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddServerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyToken_submitDisabled_iliPokazyvaetOshibku() {
        composeRule.setAleAppContent {
            AddServerScreen()
        }

        composeRule.onNodeWithTag("add_server_submit_button").performClick()

        composeRule.onNodeWithTag("add_server_error").assertIsDisplayed()
    }

    @Test
    fun validToken_proceedsToAuthChoice() {
        var proceeded = false

        composeRule.setAleAppContent {
            AddServerScreen(onConnect = { proceeded = true })
        }

        composeRule.onNodeWithTag("add_server_token_input", useUnmergedTree = true)
            .performTextInput("server.example.com:3000/ABCD1234")
        composeRule.onNodeWithTag("add_server_submit_button").performClick()

        assertTrue(proceeded)
    }
}
