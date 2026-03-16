package com.callapp.android.ui.screens.server

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.JoinRequestStatus
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserRole
import com.callapp.android.domain.model.UserStatus
import com.callapp.android.ui.common.UiState
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val server = Server(
        id = "server-1",
        name = "Tech Community",
        username = "@tech",
        description = "Team calls",
        address = "tech.example",
    )

    private val anna = User(
        id = "user-1",
        name = "Анна Смирнова",
        username = "@anna",
        role = UserRole.ADMIN,
        status = UserStatus.ONLINE,
        serverId = "server-1",
    )
    private val maria = User(
        id = "user-2",
        name = "Мария Иванова",
        username = "@maria",
        status = UserStatus.INVISIBLE,
        serverId = "server-1",
    )

    @Test
    fun adminModeShowsAdminBadgeAndRequests() {
        composeRule.setAleAppContent {
            ServerDetailScreen(
                server = server,
                membersState = UiState.Success(listOf(anna)),
                isAdmin = true,
                pendingRequests = listOf(
                    JoinRequest(
                        id = "req-1",
                        userName = "Илья",
                        username = "@ilya",
                        serverId = "server-1",
                        status = JoinRequestStatus.PENDING,
                        createdAt = "2026-03-16T10:00:00Z",
                    ),
                ),
            )
        }

        composeRule.onNodeWithText("Вы администратор").assertIsDisplayed()
        composeRule.onNodeWithText("Анна Смирнова").assertIsDisplayed()
    }

    @Test
    fun searchFieldIsVisible() {
        composeRule.setAleAppContent {
            ServerDetailScreen(
                server = server,
                membersState = UiState.Success(listOf(anna, maria)),
                isAdmin = false,
                pendingRequests = emptyList(),
            )
        }

        composeRule.onNodeWithText("Поиск участников...", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun adminModeShowsEditAction() {
        composeRule.setAleAppContent {
            ServerDetailScreen(
                server = server,
                membersState = UiState.Success(listOf(anna, maria)),
                isAdmin = true,
                currentUserId = "user-1",
                pendingRequests = emptyList(),
            )
        }

        composeRule.onNodeWithContentDescription("Редактировать участников").assertIsDisplayed()
    }

    @Test
    fun disconnectDialogConfirmsAction() {
        var disconnected = false

        composeRule.setAleAppContent {
            ServerDetailScreen(
                server = server,
                membersState = UiState.Success(listOf(anna)),
                isAdmin = true,
                pendingRequests = emptyList(),
                onDisconnectServer = { disconnected = true },
            )
        }

        composeRule.onNodeWithContentDescription("Отключить сервер").performClick()
        composeRule.onNodeWithText("Отключить сервер").assertIsDisplayed()
        composeRule.onNodeWithText("Отключить").performClick()

        assertTrue(disconnected)
    }
}
