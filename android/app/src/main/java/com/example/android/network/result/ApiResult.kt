package com.example.android.network.result

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
}

enum class ApiError {
    NetworkError,
    Unauthorized,
    ServerError,
}
