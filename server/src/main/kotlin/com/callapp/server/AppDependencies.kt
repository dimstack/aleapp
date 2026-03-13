package com.callapp.server

import com.callapp.server.auth.JwtService
import com.callapp.server.config.AppConfig
import com.callapp.server.repository.FavoriteRepository
import com.callapp.server.repository.InviteTokenRepository
import com.callapp.server.repository.JoinRequestRepository
import com.callapp.server.repository.LoginAttemptRepository
import com.callapp.server.repository.NotificationRepository
import com.callapp.server.repository.ServerRepository
import com.callapp.server.repository.UserRepository
import com.callapp.server.service.InviteTokenParser
import com.callapp.server.service.InviteTokenService
import com.callapp.server.service.ManagementService
import com.callapp.server.service.OnboardingService
import com.callapp.server.service.PasswordService
import javax.sql.DataSource
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.util.AttributeKey

data class AppDependencies(
    val config: AppConfig,
    val database: DataSource,
    val jwtService: JwtService,
    val passwordService: PasswordService,
    val inviteTokenParser: InviteTokenParser,
    val inviteTokenService: InviteTokenService,
    val onboardingService: OnboardingService,
    val managementService: ManagementService,
    val serverRepository: ServerRepository,
    val userRepository: UserRepository,
    val inviteTokenRepository: InviteTokenRepository,
    val loginAttemptRepository: LoginAttemptRepository,
    val joinRequestRepository: JoinRequestRepository,
    val favoriteRepository: FavoriteRepository,
    val notificationRepository: NotificationRepository,
)

class AppDependenciesPluginConfig {
    lateinit var config: AppConfig
    lateinit var database: DataSource
    lateinit var jwtService: JwtService
    lateinit var passwordService: PasswordService
    lateinit var inviteTokenParser: InviteTokenParser
    lateinit var inviteTokenService: InviteTokenService
    lateinit var onboardingService: OnboardingService
    lateinit var managementService: ManagementService
    lateinit var serverRepository: ServerRepository
    lateinit var userRepository: UserRepository
    lateinit var inviteTokenRepository: InviteTokenRepository
    lateinit var loginAttemptRepository: LoginAttemptRepository
    lateinit var joinRequestRepository: JoinRequestRepository
    lateinit var favoriteRepository: FavoriteRepository
    lateinit var notificationRepository: NotificationRepository
}

val AppDependenciesKey = AttributeKey<AppDependencies>("app-dependencies")

val AppDependenciesPlugin = createApplicationPlugin(
    name = "AppDependenciesPlugin",
    createConfiguration = ::AppDependenciesPluginConfig,
) {
    application.attributes.put(
        AppDependenciesKey,
        AppDependencies(
            config = pluginConfig.config,
            database = pluginConfig.database,
            jwtService = pluginConfig.jwtService,
            passwordService = pluginConfig.passwordService,
            inviteTokenParser = pluginConfig.inviteTokenParser,
            inviteTokenService = pluginConfig.inviteTokenService,
            onboardingService = pluginConfig.onboardingService,
            managementService = pluginConfig.managementService,
            serverRepository = pluginConfig.serverRepository,
            userRepository = pluginConfig.userRepository,
            inviteTokenRepository = pluginConfig.inviteTokenRepository,
            loginAttemptRepository = pluginConfig.loginAttemptRepository,
            joinRequestRepository = pluginConfig.joinRequestRepository,
            favoriteRepository = pluginConfig.favoriteRepository,
            notificationRepository = pluginConfig.notificationRepository,
        ),
    )
}

val Application.dependencies: AppDependencies
    get() = attributes[AppDependenciesKey]
