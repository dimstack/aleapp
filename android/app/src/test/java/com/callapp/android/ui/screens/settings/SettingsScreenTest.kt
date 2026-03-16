package com.callapp.android.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCurrentSectionsAndAboutContent() {
        composeRule.setAleAppContent {
            SettingsScreen(
                isDarkTheme = false,
                userStatus = UserStatus.ONLINE,
            )
        }

        composeRule.onNodeWithText("Настройки").assertIsDisplayed()
        composeRule.onNodeWithText("Тема оформления").assertIsDisplayed()
        composeRule.onNodeWithText("Статус").assertIsDisplayed()
        composeRule.onNodeWithText("Светлая").assertIsDisplayed()
        composeRule.onNodeWithText("Не беспокоить").assertIsDisplayed()
    }

    @Test
    fun clickingDarkThemeCallsCallback() {
        var selectedTheme: Boolean? = null

        composeRule.setAleAppContent {
            SettingsScreen(
                isDarkTheme = false,
                onThemeChange = { selectedTheme = it },
            )
        }

        composeRule.onNodeWithText("Темная").performClick()

        assertEquals(true, selectedTheme)
    }

    @Test
    fun clickingStatusCallsCallback() {
        var selectedStatus: UserStatus? = null

        composeRule.setAleAppContent {
            SettingsScreen(
                userStatus = UserStatus.ONLINE,
                onStatusChange = { selectedStatus = it },
            )
        }

        composeRule.onNodeWithText("Не беспокоить").performClick()

        assertEquals(UserStatus.DO_NOT_DISTURB, selectedStatus)
    }
}
