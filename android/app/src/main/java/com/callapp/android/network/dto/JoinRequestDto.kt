package com.callapp.android.network.dto

import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.JoinRequestStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JoinRequestDto(
    val id: String,
    @SerialName("user_name") val userName: String,
    val username: String,
    @SerialName("server_id") val serverId: String = "",
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String = "",
)

fun JoinRequestDto.toDomain(): JoinRequest = JoinRequest(
    id = id,
    userName = userName,
    username = username,
    serverId = serverId,
    status = when (status) {
        "approved" -> JoinRequestStatus.APPROVED
        "declined" -> JoinRequestStatus.DECLINED
        else -> JoinRequestStatus.PENDING
    },
    createdAt = createdAt,
)

@Serializable
data class SubmitJoinRequest(
    val username: String,
    val name: String,
)

@Serializable
data class JoinRequestAction(
    val status: String,
)
