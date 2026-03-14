package com.callapp.server.routes

import kotlinx.serialization.Serializable

@Serializable
data class TurnCredentialsDto(
    val urls: List<String>,
    val username: String,
    val credential: String,
)
