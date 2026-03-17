package com.callapp.android.ui.screens.home

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserStatus
import com.callapp.android.ui.common.UiState
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val server = Server(
        id = "server-1",
        name = "Tech Community",
        username = "@tech",
        address = "https://tech.example",
        availabilityStatus = ServerAvailabilityStatus.AVAILABLE,
    )

    private val favorite = User(
        id = "user-1",
        name = "Анна Смирнова",
        username = "@anna",
        status = UserStatus.ONLINE,
        serverId = "server-1",
    )

    private val favoriteItem = FavoriteContactItem(
        user = favorite,
        serverName = server.name,
        serverUsername = server.username,
        serverImageUrl = server.imageUrl,
    )

    @Test
    fun showsServerList() {
        composeRule.setAleAppContent {
            HomeScreen(
                favoritesState = UiState.Success(emptyList()),
                serversState = UiState.Success(listOf(server)),
                notificationCount = 0,
            )
        }

        composeRule.onNodeWithText("Tech Community").assertIsDisplayed()
    }

    @Test
    fun showsFavoritesWithServerUsername() {
        composeRule.setAleAppContent {
            HomeScreen(
                favoritesState = UiState.Success(listOf(favoriteItem)),
                serversState = UiState.Success(emptyList()),
                notificationCount = 0,
            )
        }

        composeRule.onNodeWithText("Анна Смирнова").assertIsDisplayed()
        composeRule.onNodeWithText("@anna").assertIsDisplayed()
        composeRule.onNodeWithText("tech").assertIsDisplayed()
    }

    @Test
    fun notificationBadge_showsCountWhenUnread() {
        composeRule.setAleAppContent {
            HomeScreen(
                favoritesState = UiState.Success(listOf(favoriteItem)),
                serversState = UiState.Success(listOf(server)),
                notificationCount = 7,
            )
        }

        composeRule.onAllNodesWithContentDescription("unread_notifications_7", useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun tapServer_navigatesToDetail() {
        composeRule.setAleAppContent {
            var route by remember { mutableStateOf("home") }

            when (route) {
                "home" -> HomeScreen(
                    favoritesState = UiState.Success(emptyList()),
                    serversState = UiState.Success(listOf(server)),
                    notificationCount = 0,
                    onServerClick = { route = "detail" },
                )
                else -> Text("Server detail")
            }
        }

        composeRule.onNodeWithText("Tech Community").performClick()

        composeRule.onNodeWithText("Server detail").assertIsDisplayed()
    }

    @Test
    fun tapFavoriteCallButton_startsCall() {
        composeRule.setAleAppContent {
            var route by remember { mutableStateOf("home") }

            when (route) {
                "home" -> HomeScreen(
                    favoritesState = UiState.Success(listOf(favoriteItem)),
                    serversState = UiState.Success(emptyList()),
                    notificationCount = 0,
                    onCallClick = { _, _, _ -> route = "call" },
                )
                else -> Text("Call screen")
            }
        }

        composeRule.onNodeWithTag("favorite_call_user-1").performClick()

        composeRule.onNodeWithText("Call screen").assertIsDisplayed()
    }

    @Test
    fun showsUnavailableStateOnlyForAffectedFavorite() {
        composeRule.setAleAppContent {
            HomeScreen(
                favoritesState = UiState.Success(
                    listOf(
                        favoriteItem.copy(
                            serverAvailabilityStatus = ServerAvailabilityStatus.UNAVAILABLE,
                            serverAvailabilityMessage = "Сервер временно недоступен",
                        ),
                    ),
                ),
                serversState = UiState.Success(listOf(server)),
                notificationCount = 0,
            )
        }

        composeRule.onNodeWithText("@anna").assertIsDisplayed()
        composeRule.onNodeWithText("tech").assertIsDisplayed()
        composeRule.onNodeWithText("Сервер временно недоступен").assertIsDisplayed()
    }
}
