package com.callapp.android.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    @SerialName("invite_token") val inviteToken: String,
    @SerialName("display_name") val displayName: String,
)
