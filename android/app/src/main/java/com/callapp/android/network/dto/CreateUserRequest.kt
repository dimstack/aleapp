package com.callapp.android.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val name: String,
    val username: String,
    val password: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val status: String? = null,
)
