package com.callapp.android.ui.screens.connect

import com.callapp.android.network.result.ApiError
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectViewModelErrorMappingTest {

    @Test
    fun `create profile shows username taken message`() {
        assertEquals(
            "Username уже занят",
            createProfileErrorMessage(ApiError.UsernameTaken("Username is already taken")),
        )
    }

    @Test
    fun `create profile shows validation message`() {
        assertEquals(
            "Password must be at least 8 characters long",
            createProfileErrorMessage(ApiError.ValidationError("Password must be at least 8 characters long")),
        )
    }

    @Test
    fun `login shows lockout message`() {
        assertEquals(
            "Слишком много попыток входа. Попробуйте через 15 минут.",
            loginErrorMessage(ApiError.LoginLocked("Too many failed login attempts")),
        )
    }

    @Test
    fun `login network error is not invalid credentials`() {
        assertEquals(
            "Нет соединения с сервером",
            loginErrorMessage(ApiError.NetworkError),
        )
    }

    @Test
    fun `connect invalid token is not generic server error`() {
        assertEquals(
            "Токен приглашения недействителен",
            connectErrorMessage(
                ApiError.Unauthorized(
                    code = "invite_token_invalid",
                    message = "Invite token is invalid",
                ),
            ),
        )
    }
}
