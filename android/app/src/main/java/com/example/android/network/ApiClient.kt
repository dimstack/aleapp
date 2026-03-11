package com.example.android.network

import com.example.android.domain.model.User
import com.example.android.network.dto.AuthRequest
import com.example.android.network.dto.AuthResponse
import com.example.android.network.dto.UserDto
import com.example.android.network.dto.toDomain
import com.example.android.network.result.ApiError
import com.example.android.network.result.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.serialization.json.Json

class ApiClient(private val baseUrl: String) {

    var sessionToken: String? = null
        internal set

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        defaultRequest {
            url(baseUrl.trimEnd('/') + "/")
            contentType(ContentType.Application.Json)
            sessionToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

    /** GET /api/users — список пользователей сервера. */
    suspend fun getUsers(): ApiResult<List<User>> = request {
        val dtos: List<UserDto> = httpClient.get("api/users").body()
        dtos.map { it.toDomain() }
    }

    /** POST /api/auth — авторизация по invite-токену. */
    suspend fun auth(inviteToken: String, displayName: String): ApiResult<AuthResponse> = request {
        val response: AuthResponse = httpClient.post("api/auth") {
            setBody(AuthRequest(inviteToken = inviteToken, displayName = displayName))
        }.body()
        sessionToken = response.sessionToken
        response
    }

    private fun <T> handleError(e: ClientRequestException): ApiResult<T> {
        if (e.response.status == HttpStatusCode.Unauthorized) {
            sessionToken = null
            return ApiResult.Failure(ApiError.Unauthorized)
        }
        return ApiResult.Failure(ApiError.ServerError)
    }

    private suspend inline fun <T> request(block: () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (e: ClientRequestException) {
            handleError(e)
        } catch (_: ServerResponseException) {
            ApiResult.Failure(ApiError.ServerError)
        } catch (_: IOException) {
            ApiResult.Failure(ApiError.NetworkError)
        }
    }

    fun close() {
        httpClient.close()
    }
}
