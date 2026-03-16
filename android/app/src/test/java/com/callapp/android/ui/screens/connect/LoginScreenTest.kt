package com.callapp.android.ui.screens.connect

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun wrongCredentials_showsError() {
        composeRule.setAleAppContent {
            LoginScreen(
                serverName = "Tech Community",
                externalError = "Invalid credentials",
            )
        }

        composeRule.onAllNodesWithTag("login_error", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun success_navigatesHome() {
        val loggedIn = AtomicBoolean(false)

        composeRule.setAleAppContent {
            LoginScreen(
                serverName = "Tech Community",
                initialUsername = "alex",
                initialPassword = "password123",
                triggerSubmitOnLaunch = true,
                onLogin = { _, _ -> loggedIn.set(true) },
            )
        }
        composeRule.waitForIdle()

        assertTrue(loggedIn.get())
    }
}
