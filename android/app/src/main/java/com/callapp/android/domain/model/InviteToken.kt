package com.callapp.android.domain.model

data class InviteToken(
    val id: String,
    val code: String,
    val label: String,
    val maxUses: Int,
    val useCount: Int,
    val grantedRole: UserRole = UserRole.MEMBER,
    val requireApproval: Boolean = false,
    val revoked: Boolean = false,
    val createdAt: String = "",
)
