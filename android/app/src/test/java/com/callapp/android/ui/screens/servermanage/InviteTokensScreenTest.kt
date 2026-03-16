package com.callapp.android.ui.screens.servermanage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.callapp.android.domain.model.InviteToken
import com.callapp.android.domain.model.UserRole
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
class InviteTokensScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val activeToken = InviteToken(
        id = "token-1",
        code = "ABCD1234",
        label = "Для команды дизайна",
        maxUses = 10,
        useCount = 2,
        grantedRole = UserRole.ADMIN,
        requireApproval = true,
    )

    @Test
    fun showsTokenListAndMeta() {
        composeRule.setAleAppContent {
            InviteTokensScreen(
                tokens = listOf(activeToken),
                serverAddress = "https://callapp.example",
            )
        }

        composeRule.onNodeWithText("Токены приглашений").assertIsDisplayed()
        composeRule.onNodeWithText("Для команды дизайна").assertIsDisplayed()
        composeRule.onNodeWithText("Админ").assertIsDisplayed()
        composeRule.onNodeWithText("С одобрением").assertIsDisplayed()
    }

    @Test
    fun copyAndRevokeCallbacksAreTriggered() {
        var copiedValue: String? = null
        var revokedId: String? = null

        composeRule.setAleAppContent {
            InviteTokensScreen(
                tokens = listOf(activeToken),
                serverAddress = "https://callapp.example",
                onCopyToken = { copiedValue = it },
                onRevokeToken = { revokedId = it },
            )
        }

        composeRule.onNodeWithContentDescription("Копировать").performClick()
        composeRule.onNodeWithText("Отозвать").performClick()

        assertEquals("callapp.example/ABCD1234", copiedValue)
        assertEquals("token-1", revokedId)
    }

    @Test
    fun floatingActionButtonIsVisible() {
        composeRule.setAleAppContent {
            InviteTokensScreen(
                tokens = emptyList(),
                serverAddress = "server.example",
            )
        }

        composeRule.onNodeWithContentDescription("Создать токен").assertIsDisplayed()
    }

    @Test
    fun emptyStateIsShownWithoutTokens() {
        composeRule.setAleAppContent {
            InviteTokensScreen(
                tokens = emptyList(),
                serverAddress = "server.example",
            )
        }

        composeRule.onNodeWithText("Нет токенов").assertIsDisplayed()
        composeRule.onNodeWithText("Создайте токен приглашения для новых участников").assertIsDisplayed()
    }
}
