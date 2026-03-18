package com.callapp.android.ui.screens.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MyProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersAdminProfileWithoutCrashing() {
        composeRule.setAleAppContent {
            MyProfileScreen(
                profile = MyProfileData(
                    name = "Alex Admin",
                    username = "alex_admin",
                    avatarUrl = "",
                    serverName = "Tech Community",
                    isAdmin = true,
                ),
            )
        }

        composeRule.waitForIdle()
    }

    @Test
    fun rendersMemberProfileWithoutCrashing() {
        composeRule.setAleAppContent {
            MyProfileScreen(
                profile = MyProfileData(
                    name = "Maria",
                    username = "maria",
                    avatarUrl = "",
                    serverName = "Creative Studio",
                    isAdmin = false,
                ),
            )
        }

        composeRule.waitForIdle()
    }

    @Test
    fun rendersSingleAtSignWhenUsernameAlreadyContainsPrefix() {
        composeRule.setAleAppContent {
            MyProfileScreen(
                profile = MyProfileData(
                    name = "Alex Admin",
                    username = "@alex_admin",
                    avatarUrl = "",
                    serverName = "Tech Community",
                    isAdmin = true,
                ),
            )
        }

        composeRule.onAllNodesWithText("@alex_admin").assertCountEquals(2)
        composeRule.onAllNodesWithText("@@alex_admin").assertCountEquals(0)
    }

    @Test
    fun changePhotoButton_opensPhotoDialogInEditMode() {
        var pickerRequested = false

        composeRule.setAleAppContent {
            MyProfileScreen(
                profile = MyProfileData(
                    name = "Alex Admin",
                    username = "alex_admin",
                    avatarUrl = "",
                    serverName = "Tech Community",
                    isAdmin = true,
                ),
                onPhotoPickerRequest = { pickerRequested = true },
            )
        }

        composeRule.onNodeWithText("Редактировать профиль").performClick()
        composeRule.onNodeWithContentDescription("Изменить фото").performClick()
        assertTrue(pickerRequested)
    }
}
