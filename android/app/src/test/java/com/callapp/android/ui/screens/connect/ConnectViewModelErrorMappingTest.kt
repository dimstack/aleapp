package com.callapp.android.ui.screens.connect

import com.callapp.android.network.result.ApiError
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectViewModelErrorMappingTest {

    @Test
    fun `create profile shows username taken message`() {
        assertEquals(
            "Username \u0443\u0436\u0435 \u0437\u0430\u043D\u044F\u0442",
            createProfileErrorMessage(ApiError.UsernameTaken("Username is already taken")),
        )
    }

    @Test
    fun `create profile shows validation message`() {
        assertEquals(
            "\u041F\u0430\u0440\u043E\u043B\u044C \u0434\u043E\u043B\u0436\u0435\u043D \u0441\u043E\u0434\u0435\u0440\u0436\u0430\u0442\u044C \u043C\u0438\u043D\u0438\u043C\u0443\u043C 8 \u0441\u0438\u043C\u0432\u043E\u043B\u043E\u0432",
            createProfileErrorMessage(ApiError.ValidationError("Password must be at least 8 characters long")),
        )
    }

    @Test
    fun `login shows lockout message`() {
        assertEquals(
            "\u0421\u043B\u0438\u0448\u043A\u043E\u043C \u043C\u043D\u043E\u0433\u043E \u043F\u043E\u043F\u044B\u0442\u043E\u043A \u0432\u0445\u043E\u0434\u0430. \u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435 \u0447\u0435\u0440\u0435\u0437 15 \u043C\u0438\u043D\u0443\u0442.",
            loginErrorMessage(ApiError.LoginLocked("Too many failed login attempts")),
        )
    }

    @Test
    fun `login network error is not invalid credentials`() {
        assertEquals(
            "\u041D\u0435\u0442 \u0441\u043E\u0435\u0434\u0438\u043D\u0435\u043D\u0438\u044F \u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u043E\u043C",
            loginErrorMessage(ApiError.NetworkError),
        )
    }

    @Test
    fun `login invite token revoked shows specific message`() {
        assertEquals(
            "\u0422\u043E\u043A\u0435\u043D \u043F\u0440\u0438\u0433\u043B\u0430\u0448\u0435\u043D\u0438\u044F \u043E\u0442\u043E\u0437\u0432\u0430\u043D",
            loginErrorMessage(
                ApiError.Unauthorized(
                    code = "invite_token_revoked",
                    message = "Invite token is revoked",
                ),
            ),
        )
    }

    @Test
    fun `login invite token exhausted shows specific message`() {
        assertEquals(
            "\u041B\u0438\u043C\u0438\u0442 \u0438\u0441\u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u043D\u0438\u044F \u0442\u043E\u043A\u0435\u043D\u0430 \u0438\u0441\u0447\u0435\u0440\u043F\u0430\u043D",
            loginErrorMessage(
                ApiError.Unauthorized(
                    code = "invite_token_exhausted",
                    message = "Invite token exhausted",
                ),
            ),
        )
    }

    @Test
    fun `connect invalid token is not generic server error`() {
        assertEquals(
            "\u0422\u043E\u043A\u0435\u043D \u043F\u0440\u0438\u0433\u043B\u0430\u0448\u0435\u043D\u0438\u044F \u043D\u0435\u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0442\u0435\u043B\u0435\u043D",
            connectErrorMessage(
                ApiError.Unauthorized(
                    code = "invite_token_invalid",
                    message = "Invite token is invalid",
                ),
            ),
        )
    }
}
