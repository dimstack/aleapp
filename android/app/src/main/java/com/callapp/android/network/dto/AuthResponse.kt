package com.callapp.android.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    @SerialName("session_token") val sessionToken: String,
    val user: UserDto? = null,
    val server: ServerDto? = null,
    val status: String = "joined",
) {
    val isJoined: Boolean get() = status == "joined"
    val isPending: Boolean get() = status == "pending"
    val needsProfile: Boolean get() = status == "needs_profile"
}
