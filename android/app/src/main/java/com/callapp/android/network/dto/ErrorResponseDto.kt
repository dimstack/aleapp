package com.callapp.android.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ErrorResponseDto(
    val code: String,
    val message: String,
    val details: JsonElement? = null,
)
