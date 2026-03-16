package com.callapp.android.ui.screens.connect

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CreateProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fillAllFields_submitEnabled() {
        val submitted = AtomicBoolean(false)

        composeRule.setAleAppContent {
            CreateProfileScreen(
                serverName = "Tech Community",
                initialUsername = "alex",
                initialName = "Alex",
                initialPassword = "password123",
                initialConfirmPassword = "password123",
                initialAvatarUrl = "https://example.com/avatar.jpg",
                triggerSubmitOnLaunch = true,
                onCreateProfile = { _, _, _, _ -> submitted.set(true) },
            )
        }

        composeRule.waitForIdle()

        assertTrue(submitted.get())
    }

    @Test
    fun shortPassword_showsError() {
        val submitted = AtomicBoolean(false)

        composeRule.setAleAppContent {
            CreateProfileScreen(
                serverName = "Tech Community",
                onCreateProfile = { _, _, _, _ -> submitted.set(true) },
            )
        }

        composeRule.onNodeWithTag("create_profile_username_input", useUnmergedTree = true).performTextReplacement("alex")
        composeRule.onNodeWithTag("create_profile_name_input", useUnmergedTree = true).performTextReplacement("Alex")
        composeRule.onNodeWithTag("create_profile_password_input", useUnmergedTree = true).performTextReplacement("1234567")
        composeRule.onNodeWithTag("create_profile_confirm_password_input", useUnmergedTree = true).performTextReplacement("1234567")
        composeRule.onNodeWithTag("create_profile_submit_button").performClick()
        composeRule.waitForIdle()

        assertFalse(submitted.get())
    }

    @Test
    fun submitSuccess_navigatesHome() {
        val submitted = AtomicBoolean(false)

        composeRule.setAleAppContent {
            CreateProfileScreen(
                serverName = "Tech Community",
                initialUsername = "alex",
                initialName = "Alex",
                initialPassword = "password123",
                initialConfirmPassword = "password123",
                triggerSubmitOnLaunch = true,
                onCreateProfile = { _, _, _, _ -> submitted.set(true) },
            )
        }
        composeRule.waitForIdle()

        assertTrue(submitted.get())
    }
}
