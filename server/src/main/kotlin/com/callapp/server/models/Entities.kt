package com.callapp.server.models

import java.time.Instant

data class ServerRecord(
    val id: String,
    val name: String,
    val username: String,
    val description: String,
    val imageUrl: String?,
    val createdAt: Instant,
)

data class UserRecord(
    val id: String,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val avatarUrl: String?,
    val role: Role,
    val status: UserStatus,
    val serverId: String,
    val isApproved: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSeenAt: Instant?,
    val lockoutUntil: Instant?,
)

data class InviteTokenRecord(
    val id: String,
    val token: String,
    val label: String,
    val serverId: String,
    val createdBy: String?,
    val maxUses: Int,
    val currentUses: Int,
    val grantedRole: Role,
    val requireApproval: Boolean,
    val expiresAt: Instant?,
    val isRevoked: Boolean,
    val createdAt: Instant,
)

data class LoginAttemptRecord(
    val serverId: String,
    val username: String,
    val failedAttempts: Int,
    val lockedUntil: Instant?,
    val updatedAt: Instant,
)

data class PendingApprovalRecord(
    val id: String,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val avatarUrl: String?,
    val inviteTokenId: String,
    val serverId: String,
    val requestedRole: Role,
    val status: JoinRequestStatus,
    val createdAt: Instant,
)
