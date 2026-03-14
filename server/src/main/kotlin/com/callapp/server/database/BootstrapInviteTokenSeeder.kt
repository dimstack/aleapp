package com.callapp.server.database

import com.callapp.server.config.BootstrapConfig
import com.callapp.server.config.ServerConfig
import com.callapp.server.models.Role
import com.callapp.server.repository.InviteTokenRepository
import java.util.UUID

class BootstrapInviteTokenSeeder(
    private val inviteTokenRepository: InviteTokenRepository,
    private val serverConfig: ServerConfig,
    private val bootstrapConfig: BootstrapConfig,
) {
    fun seedAdminInviteTokenIfConfigured() {
        val rawToken = bootstrapConfig.adminInviteToken?.trim().orEmpty()
        if (rawToken.isBlank()) {
            return
        }

        require(rawToken.matches(Regex("^[A-Za-z0-9]{8,32}$"))) {
            "BOOTSTRAP_ADMIN_TOKEN must be alphanumeric and 8-32 characters long"
        }
        require(bootstrapConfig.adminInviteMaxUses > 0) {
            "BOOTSTRAP_ADMIN_TOKEN_MAX_USES must be greater than 0"
        }

        if (inviteTokenRepository.findByToken(rawToken) != null) {
            return
        }

        inviteTokenRepository.create(
            id = UUID.randomUUID().toString(),
            token = rawToken,
            label = bootstrapConfig.adminInviteLabel,
            serverId = serverConfig.id,
            createdBy = null,
            maxUses = bootstrapConfig.adminInviteMaxUses,
            grantedRole = Role.ADMIN,
            requireApproval = false,
        )
    }
}
