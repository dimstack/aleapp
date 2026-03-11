package com.example.android.network

import com.example.android.domain.model.JoinRequest
import com.example.android.domain.model.Server
import com.example.android.domain.model.User
import com.example.android.network.dto.AuthRequest
import com.example.android.network.dto.AuthResponse
import com.example.android.network.dto.ConnectRequest
import com.example.android.network.dto.ConnectResponse
import com.example.android.network.dto.CreateUserRequest
import com.example.android.network.dto.JoinRequestAction
import com.example.android.network.dto.JoinRequestDto
import com.example.android.network.dto.NotificationDto
import com.example.android.network.dto.ServerDto
import com.example.android.network.dto.SubmitJoinRequest
import com.example.android.network.dto.UpdateServerRequest
import com.example.android.network.dto.UpdateUserRequest
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
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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

    // ── Authentication ───────────────────────────────────────────────────

    /** POST /api/connect — подключение к серверу (с опциональным API-ключом для админа). */
    suspend fun connect(apiKey: String? = null): ApiResult<ConnectResponse> = request {
        val response: ConnectResponse = httpClient.post("api/connect") {
            setBody(ConnectRequest(apiKey = apiKey))
        }.body()
        response.sessionToken?.let { sessionToken = it }
        response
    }

    /** POST /api/auth — авторизация по invite-токену. */
    suspend fun auth(inviteToken: String, displayName: String): ApiResult<AuthResponse> = request {
        val response: AuthResponse = httpClient.post("api/auth") {
            setBody(AuthRequest(inviteToken = inviteToken, displayName = displayName))
        }.body()
        sessionToken = response.sessionToken
        response
    }

    // ── Users ────────────────────────────────────────────────────────────

    /** POST /api/users — создание профиля на сервере. */
    suspend fun createUser(name: String, username: String, avatarUrl: String? = null): ApiResult<User> = request {
        val dto: UserDto = httpClient.post("api/users") {
            setBody(CreateUserRequest(name = name, username = username, avatarUrl = avatarUrl))
        }.body()
        dto.toDomain()
    }

    /** GET /api/users — список пользователей сервера. */
    suspend fun getUsers(): ApiResult<List<User>> = request {
        val dtos: List<UserDto> = httpClient.get("api/users").body()
        dtos.map { it.toDomain() }
    }

    /** GET /api/users/{id} — профиль пользователя. */
    suspend fun getUser(userId: String): ApiResult<User> = request {
        val dto: UserDto = httpClient.get("api/users/$userId").body()
        dto.toDomain()
    }

    /** PUT /api/users/{id} — обновление профиля. */
    suspend fun updateUser(
        userId: String,
        name: String? = null,
        username: String? = null,
        avatarUrl: String? = null,
        status: String? = null,
    ): ApiResult<User> = request {
        val dto: UserDto = httpClient.put("api/users/$userId") {
            setBody(UpdateUserRequest(name = name, username = username, avatarUrl = avatarUrl, status = status))
        }.body()
        dto.toDomain()
    }

    /** DELETE /api/users/{id} — удалить пользователя с сервера (только админ). */
    suspend fun deleteUser(userId: String): ApiResult<Unit> = request {
        httpClient.delete("api/users/$userId")
    }

    // ── Server Management ────────────────────────────────────────────────

    /** GET /api/server — информация о сервере. */
    suspend fun getServer(): ApiResult<Server> = request {
        val dto: ServerDto = httpClient.get("api/server").body()
        dto.toDomain(address = baseUrl)
    }

    /** PUT /api/server — обновление информации (только админ). */
    suspend fun updateServer(
        name: String? = null,
        username: String? = null,
        description: String? = null,
        imageUrl: String? = null,
    ): ApiResult<Server> = request {
        val dto: ServerDto = httpClient.put("api/server") {
            setBody(UpdateServerRequest(name = name, username = username, description = description, imageUrl = imageUrl))
        }.body()
        dto.toDomain(address = baseUrl)
    }

    /** DELETE /api/server — удалить сервер (только админ). */
    suspend fun deleteServer(): ApiResult<Unit> = request {
        httpClient.delete("api/server")
    }

    // ── Join Requests ────────────────────────────────────────────────────

    /** POST /api/join-requests — отправить заявку на вступление. */
    suspend fun submitJoinRequest(username: String, name: String): ApiResult<JoinRequest> = request {
        val dto: JoinRequestDto = httpClient.post("api/join-requests") {
            setBody(SubmitJoinRequest(username = username, name = name))
        }.body()
        dto.toDomain()
    }

    /** GET /api/join-requests — список заявок (только админ). */
    suspend fun getJoinRequests(): ApiResult<List<JoinRequest>> = request {
        val dtos: List<JoinRequestDto> = httpClient.get("api/join-requests").body()
        dtos.map { it.toDomain() }
    }

    /** PUT /api/join-requests/{id} — одобрить/отклонить заявку (только админ). */
    suspend fun updateJoinRequest(requestId: String, status: String): ApiResult<JoinRequest> = request {
        val dto: JoinRequestDto = httpClient.put("api/join-requests/$requestId") {
            setBody(JoinRequestAction(status = status))
        }.body()
        dto.toDomain()
    }

    // ── Favorites ────────────────────────────────────────────────────────

    /** GET /api/favorites — список избранных контактов. */
    suspend fun getFavorites(): ApiResult<List<User>> = request {
        val dtos: List<UserDto> = httpClient.get("api/favorites").body()
        dtos.map { it.toDomain() }
    }

    /** POST /api/favorites/{userId} — добавить в избранное. */
    suspend fun addFavorite(userId: String): ApiResult<Unit> = request {
        httpClient.post("api/favorites/$userId")
    }

    /** DELETE /api/favorites/{userId} — убрать из избранного. */
    suspend fun removeFavorite(userId: String): ApiResult<Unit> = request {
        httpClient.delete("api/favorites/$userId")
    }

    // ── Notifications ────────────────────────────────────────────────────

    /** GET /api/notifications — уведомления пользователя. */
    suspend fun getNotifications(): ApiResult<List<NotificationDto>> = request {
        httpClient.get("api/notifications").body()
    }

    /** PUT /api/notifications/read — прочитать все уведомления. */
    suspend fun markNotificationsRead(): ApiResult<Unit> = request {
        httpClient.put("api/notifications/read")
    }

    /** DELETE /api/notifications — очистить все уведомления. */
    suspend fun clearNotifications(): ApiResult<Unit> = request {
        httpClient.delete("api/notifications")
    }

    // ── Error handling ───────────────────────────────────────────────────

    private fun <T> handleError(e: ClientRequestException): ApiResult<T> {
        return when (e.response.status) {
            HttpStatusCode.Unauthorized -> {
                sessionToken = null
                ApiResult.Failure(ApiError.Unauthorized)
            }
            HttpStatusCode.NotFound -> ApiResult.Failure(ApiError.NotFound)
            else -> ApiResult.Failure(ApiError.ServerError)
        }
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
