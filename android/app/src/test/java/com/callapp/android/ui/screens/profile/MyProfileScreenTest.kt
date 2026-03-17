package com.callapp.android.ui.screens.profile

import androidx.compose.ui.test.junit4.createComposeRule
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Rule
import org.junit.Test
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
                    serverName = "Creative Studio",
                    isAdmin = false,
                ),
            )
        }

        composeRule.waitForIdle()
    }
}
