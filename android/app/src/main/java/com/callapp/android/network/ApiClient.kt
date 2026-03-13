package com.callapp.android.network

import com.callapp.android.domain.model.InviteToken
import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.User
import com.callapp.android.network.dto.AuthResponse
import com.callapp.android.network.dto.ConnectRequest
import com.callapp.android.network.dto.ConnectResponse
import com.callapp.android.network.dto.CreateInviteTokenRequest
import com.callapp.android.network.dto.CreateUserRequest
import com.callapp.android.network.dto.InviteTokenDto
import com.callapp.android.network.dto.JoinRequestAction
import com.callapp.android.network.dto.JoinRequestDto
import com.callapp.android.network.dto.LoginRequest
import com.callapp.android.network.dto.NotificationDto
import com.callapp.android.network.dto.ServerDto
import com.callapp.android.network.dto.TurnCredentialsDto
import com.callapp.android.network.dto.UpdateServerRequest
import com.callapp.android.network.dto.UpdateUserRequest
import com.callapp.android.network.dto.UserDto
import com.callapp.android.network.dto.toDomain
import com.callapp.android.network.result.ApiError
import com.callapp.android.network.result.ApiResult
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
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        defaultRequest {
            url(baseUrl.trimEnd('/') + "/")
            contentType(ContentType.Application.Json)
            sessionToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

    suspend fun connect(inviteToken: String): ApiResult<ConnectResponse> = request {
        val response: ConnectResponse = httpClient.post("api/connect") {
            setBody(ConnectRequest(token = inviteToken))
        }.body()
        sessionToken = response.sessionToken
        response
    }

    suspend fun login(
        inviteToken: String,
        username: String,
        password: String,
    ): ApiResult<AuthResponse> = request {
        val response: AuthResponse = httpClient.post("api/auth/login") {
            setBody(LoginRequest(inviteToken = inviteToken, username = username, password = password))
        }.body()
        sessionToken = response.sessionToken
        response
    }

    suspend fun createUser(
        name: String,
        username: String,
        password: String,
        avatarUrl: String? = null,
    ): ApiResult<User> = request {
        val dto: UserDto = httpClient.post("api/users") {
            setBody(CreateUserRequest(name = name, username = username, password = password, avatarUrl = avatarUrl))
        }.body()
        dto.toDomain()
    }

    suspend fun getUsers(): ApiResult<List<User>> = request {
        val dtos: List<UserDto> = httpClient.get("api/users").body()
        dtos.map { it.toDomain() }
    }

    suspend fun getUser(userId: String): ApiResult<User> = request {
        val dto: UserDto = httpClient.get("api/users/$userId").body()
        dto.toDomain()
    }

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

    suspend fun deleteUser(userId: String): ApiResult<Unit> = request {
        httpClient.delete("api/users/$userId")
    }

    suspend fun getServer(): ApiResult<Server> = request {
        val dto: ServerDto = httpClient.get("api/server").body()
        dto.toDomain(address = baseUrl)
    }

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

    suspend fun getJoinRequests(): ApiResult<List<JoinRequest>> = request {
        val dtos: List<JoinRequestDto> = httpClient.get("api/join-requests").body()
        dtos.map { it.toDomain() }
    }

    suspend fun updateJoinRequest(requestId: String, status: String): ApiResult<JoinRequest> = request {
        val dto: JoinRequestDto = httpClient.put("api/join-requests/$requestId") {
            setBody(JoinRequestAction(status = status))
        }.body()
        dto.toDomain()
    }

    suspend fun createInviteToken(
        label: String,
        maxUses: Int = 0,
        grantedRole: String = "MEMBER",
        requireApproval: Boolean = false,
    ): ApiResult<InviteToken> = request {
        val dto: InviteTokenDto = httpClient.post("api/invite-tokens") {
            setBody(
                CreateInviteTokenRequest(
                    label = label,
                    maxUses = maxUses,
                    grantedRole = grantedRole,
                    requireApproval = requireApproval,
                ),
            )
        }.body()
        dto.toDomain()
    }

    suspend fun getInviteTokens(): ApiResult<List<InviteToken>> = request {
        val dtos: List<InviteTokenDto> = httpClient.get("api/invite-tokens").body()
        dtos.map { it.toDomain() }
    }

    suspend fun revokeInviteToken(tokenId: String): ApiResult<Unit> = request {
        httpClient.delete("api/invite-tokens/$tokenId")
    }

    suspend fun getFavorites(): ApiResult<List<User>> = request {
        val dtos: List<UserDto> = httpClient.get("api/favorites").body()
        dtos.map { it.toDomain() }
    }

    suspend fun addFavorite(userId: String): ApiResult<Unit> = request {
        httpClient.post("api/favorites/$userId")
    }

    suspend fun removeFavorite(userId: String): ApiResult<Unit> = request {
        httpClient.delete("api/favorites/$userId")
    }

    suspend fun getNotifications(): ApiResult<List<com.callapp.android.domain.model.Notification>> = request {
        httpClient.get("api/notifications").body<List<NotificationDto>>().map { it.toDomain() }
    }

    suspend fun markNotificationsRead(): ApiResult<Unit> = request {
        httpClient.put("api/notifications/read")
    }

    suspend fun clearNotifications(): ApiResult<Unit> = request {
        httpClient.delete("api/notifications")
    }

    suspend fun getTurnCredentials(): ApiResult<TurnCredentialsDto> = request {
        httpClient.get("api/turn-credentials").body<TurnCredentialsDto>()
    }

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
