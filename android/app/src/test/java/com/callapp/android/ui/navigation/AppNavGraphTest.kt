package com.callapp.android.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.callapp.android.calling.IncomingCallPayload
import com.callapp.android.data.ServiceLocator
import com.callapp.android.data.SessionStore
import com.callapp.android.testutil.InMemorySharedPreferences
import com.callapp.android.ui.screens.settings.UserStatus
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppNavGraphTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        ServiceLocator.sessionStore = SessionStore.createForTests(InMemorySharedPreferences())
        ServiceLocator.activeServerAddress = ""
        ServiceLocator.currentUserId = ""

        navController = TestNavHostController(
            ApplicationProvider.getApplicationContext(),
        ).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
    }

    @Test
    fun startsFromHomeRoute() {
        composeRule.setAleAppContent {
            AppNavGraph(navController = navController)
        }

        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(Route.Home.route, navController.currentDestination?.route)
    }

    @Test
    fun navigatesToSettingsRoute() {
        composeRule.setAleAppContent {
            AppNavGraph(
                navController = navController,
                isDarkTheme = true,
                userStatus = UserStatus.DO_NOT_DISTURB,
            )
        }

        composeRule.runOnIdle {
            navController.navigate(Route.Settings.route)
        }

        assertEquals(Route.Settings.route, navController.currentDestination?.route)
    }

    @Test
    fun decodesAuthChoiceArgumentsFromRoute() {
        val serverName = "Dev Hub / QA"

        composeRule.setAleAppContent {
            AppNavGraph(navController = navController)
        }

        composeRule.runOnIdle {
            navController.navigate(Route.AuthChoice.createRoute(serverName))
        }

        composeRule.onNodeWithTag("auth_choice_create_account").assertIsDisplayed()
        assertEquals(Route.AuthChoice.route, navController.currentDestination?.route)
    }

    @Test
    fun decodesPendingRequestArgumentsFromRoute() {
        val serverName = "Creative Space"
        val userName = "@maria.test"

        composeRule.setAleAppContent {
            AppNavGraph(navController = navController)
        }

        composeRule.runOnIdle {
            navController.navigate(Route.PendingRequest.createRoute(serverName, userName))
        }

        composeRule.onNodeWithText(userName).assertIsDisplayed()
        assertEquals(Route.PendingRequest.route, navController.currentDestination?.route)
    }

    @Test
    fun pendingIncomingCallNavigatesAndConsumesPayload() {
        var consumed = false

        composeRule.setAleAppContent {
            AppNavGraph(
                navController = navController,
                pendingIncomingCall = IncomingCallPayload(
                    serverAddress = "https://server.example.com:3000",
                    userId = "user-42",
                    contactName = "Alice Smith",
                    serverName = "Alpha Team",
                ),
                onIncomingCallConsumed = { consumed = true },
            )
        }

        composeRule.runOnIdle {
            assertTrue(consumed)
            assertEquals(Route.IncomingCall.route, navController.currentDestination?.route)
        }
    }
}
