package com.callapp.server.routes

import com.callapp.server.database.DatabaseHealth
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val service: String,
    val environment: String,
    val status: String,
    val database: DatabaseHealth,
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: String? = null,
)
