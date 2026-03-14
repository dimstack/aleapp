package com.callapp.android.network.result

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
}

sealed interface ApiError {
    data object NetworkError : ApiError
    data class Unauthorized(
        val code: String? = null,
        val message: String? = null,
    ) : ApiError

    data object NotFound : ApiError
    data class ValidationError(val message: String) : ApiError
    data class UsernameTaken(val message: String) : ApiError
    data class LoginLocked(val message: String) : ApiError
    data class Forbidden(val message: String) : ApiError
    data class DeprecatedEndpoint(val message: String) : ApiError
    data class ServerError(val message: String? = null) : ApiError
}
