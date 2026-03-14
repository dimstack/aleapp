package com.callapp.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConnectRequest(
    val token: String,
)
