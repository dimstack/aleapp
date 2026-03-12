package com.callapp.android.domain.model

enum class JoinRequestStatus { PENDING, APPROVED, DECLINED }

data class JoinRequest(
    val id: String,
    val userName: String,
    val username: String,
    val serverId: String = "",
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    val createdAt: String = "",
)
