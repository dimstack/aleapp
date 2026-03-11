package com.example.android.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectRequest(
    @SerialName("api_key") val apiKey: String? = null,
)

@Serializable
data class ConnectResponse(
    @SerialName("server_name") val serverName: String,
    @SerialName("is_admin") val isAdmin: Boolean,
    @SerialName("session_token") val sessionToken: String? = null,
)
