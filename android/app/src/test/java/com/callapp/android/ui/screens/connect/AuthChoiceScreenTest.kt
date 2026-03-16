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
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthChoiceScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsServerName() {
        composeRule.setAleAppContent {
            AuthChoiceScreen(serverName = "Tech Community")
        }

        composeRule.onNodeWithText("Tech Community", substring = true).assertIsDisplayed()
    }

    @Test
    fun clickCreateAccount_navigatesToCreateProfileScreen() {
        var navigated = false

        composeRule.setAleAppContent {
            AuthChoiceScreen(
                serverName = "Tech Community",
                onCreateAccount = { navigated = true },
            )
        }

        composeRule.onNodeWithTag("auth_choice_create_account").performClick()

        assertTrue(navigated)
    }

    @Test
    fun clickLogin_navigatesToLoginScreen() {
        var navigated = false

        composeRule.setAleAppContent {
            AuthChoiceScreen(
                serverName = "Tech Community",
                onLogin = { navigated = true },
            )
        }

        composeRule.onNodeWithTag("auth_choice_login").performClick()

        assertTrue(navigated)
    }
}
