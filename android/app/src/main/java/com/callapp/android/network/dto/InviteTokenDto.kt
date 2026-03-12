package com.callapp.android.network.dto

import com.callapp.android.domain.model.InviteToken
import com.callapp.android.domain.model.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteTokenDto(
    val id: String,
    val code: String,
    val label: String,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("use_count") val useCount: Int,
    @SerialName("granted_role") val grantedRole: String = "MEMBER",
    @SerialName("require_approval") val requireApproval: Boolean = false,
    val revoked: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)

fun InviteTokenDto.toDomain(): InviteToken = InviteToken(
    id = id,
    code = code,
    label = label,
    maxUses = maxUses,
    useCount = useCount,
    grantedRole = when (grantedRole.uppercase()) {
        "ADMIN" -> UserRole.ADMIN
        else -> UserRole.MEMBER
    },
    requireApproval = requireApproval,
    revoked = revoked,
    createdAt = createdAt,
)

@Serializable
data class CreateInviteTokenRequest(
    val label: String,
    @SerialName("max_uses") val maxUses: Int = 0,
    @SerialName("granted_role") val grantedRole: String = "MEMBER",
    @SerialName("require_approval") val requireApproval: Boolean = false,
)
