package com.example.android.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("invite_token") val inviteToken: String,
    val username: String,
    val password: String,
)
