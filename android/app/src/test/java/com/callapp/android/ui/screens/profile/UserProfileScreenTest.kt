package com.callapp.android.ui.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
class UserProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersUserProfileAndFavoriteState() {
        composeRule.setAleAppContent {
            UserProfileScreen(
                user = UserProfileData(
                    userId = "user-1",
                    name = "Maria",
                    username = "maria",
                    serverName = "Creative Studio",
                    isAdmin = false,
                    isFavorite = false,
                ),
            )
        }

        composeRule.onNodeWithText("Maria").assertIsDisplayed()
        composeRule.onNodeWithText("@maria").assertIsDisplayed()
        composeRule.onNodeWithText("Добавить в избранное").assertIsDisplayed()
    }

    @Test
    fun toggleFavorite_updatesButtonTextAndCallsCallback() {
        var toggledUserId = ""
        var isFavorite by mutableStateOf(false)

        composeRule.setAleAppContent {
            UserProfileScreen(
                user = UserProfileData(
                    userId = "user-42",
                    name = "Maria",
                    username = "maria",
                    serverName = "Creative Studio",
                    isAdmin = false,
                    isFavorite = isFavorite,
                ),
                onToggleFavorite = {
                    toggledUserId = it
                    isFavorite = !isFavorite
                },
            )
        }

        composeRule.onNodeWithText("Добавить в избранное").performClick()

        assertEquals("user-42", toggledUserId)
        composeRule.onNodeWithText("Удалить из избранного").assertIsDisplayed()
    }

    @Test
    fun updatesButtonWhenFavoriteStateChangesFromViewModel() {
        var isFavorite by mutableStateOf(false)

        composeRule.setAleAppContent {
            UserProfileScreen(
                user = UserProfileData(
                    userId = "user-1",
                    name = "Maria",
                    username = "maria",
                    serverName = "Creative Studio",
                    isAdmin = false,
                    isFavorite = isFavorite,
                ),
            )
        }

        composeRule.onNodeWithText("Добавить в избранное").assertIsDisplayed()

        composeRule.runOnIdle {
            isFavorite = true
        }

        composeRule.onNodeWithText("Удалить из избранного").assertIsDisplayed()
    }
}
