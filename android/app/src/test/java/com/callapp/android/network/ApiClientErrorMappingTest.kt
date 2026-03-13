package com.callapp.android.network

import com.callapp.android.network.result.ApiError
import io.ktor.http.HttpStatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientErrorMappingTest {

    @Test
    fun `maps validation error from backend body`() {
        val error = mapApiError(
            status = HttpStatusCode.BadRequest,
            body = """{"code":"validation_error","message":"Password must be at least 8 characters long"}""",
        )

        assertEquals(
            ApiError.ValidationError("Password must be at least 8 characters long"),
            error,
        )
    }

    @Test
    fun `maps login locked from backend body`() {
        val error = mapApiError(
            status = HttpStatusCode.Unauthorized,
            body = """{"code":"login_locked","message":"Too many failed login attempts"}""",
        )

        assertEquals(
            ApiError.LoginLocked("Too many failed login attempts"),
            error,
        )
    }

    @Test
    fun `maps unauthorized from backend body`() {
        val error = mapApiError(
            status = HttpStatusCode.Unauthorized,
            body = """{"code":"unauthorized","message":"Invalid username or password"}""",
        )

        assertEquals(
            ApiError.Unauthorized(
                code = "unauthorized",
                message = "Invalid username or password",
            ),
            error,
        )
    }

    @Test
    fun `maps username taken from backend body`() {
        val error = mapApiError(
            status = HttpStatusCode.Conflict,
            body = """{"code":"username_taken","message":"Username is already taken"}""",
        )

        assertEquals(
            ApiError.UsernameTaken("Username is already taken"),
            error,
        )
    }

    @Test
    fun `maps deprecated endpoint from backend body`() {
        val error = mapApiError(
            status = HttpStatusCode.Gone,
            body = """{"code":"deprecated_endpoint","message":"Server deletion is disabled in this build"}""",
        )

        assertEquals(
            ApiError.DeprecatedEndpoint("Server deletion is disabled in this build"),
            error,
        )
    }

    @Test
    fun `returns server error for malformed body`() {
        val error = mapApiError(
            status = HttpStatusCode.BadRequest,
            body = """not-json""",
        )

        assertTrue(error is ApiError.ServerError)
    }
}
