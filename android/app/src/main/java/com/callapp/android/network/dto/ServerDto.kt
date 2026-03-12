package com.callapp.android.network.dto

import com.callapp.android.domain.model.Server
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerDto(
    val id: String,
    val name: String,
    val username: String,
    val description: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
)

fun ServerDto.toDomain(address: String = ""): Server = Server(
    id = id,
    name = name,
    username = username,
    description = description,
    imageUrl = imageUrl,
    address = address,
)

@Serializable
data class UpdateServerRequest(
    val name: String? = null,
    val username: String? = null,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)
