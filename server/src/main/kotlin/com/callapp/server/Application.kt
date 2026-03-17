package com.callapp.server

import com.callapp.server.auth.JwtService
import com.callapp.server.config.AppConfig
import com.callapp.server.database.DatabaseFactory
import com.callapp.server.database.MigrationRunner
import com.callapp.server.database.BootstrapInviteTokenSeeder
import com.callapp.server.database.ServerBootstrap
import com.callapp.server.plugins.configureAuth
import com.callapp.server.plugins.configureHTTP
import com.callapp.server.plugins.configureMonitoring
import com.callapp.server.plugins.configureRouting
import com.callapp.server.plugins.configureSerialization
import com.callapp.server.plugins.configureStatusPages
import com.callapp.server.plugins.configureWebSockets
import com.callapp.server.repository.FavoriteRepository
import com.callapp.server.repository.InviteTokenRepository
import com.callapp.server.repository.JoinRequestRepository
import com.callapp.server.repository.LoginAttemptRepository
import com.callapp.server.repository.NotificationRepository
import com.callapp.server.repository.ServerRepository
import com.callapp.server.repository.UserRepository
import com.callapp.server.signaling.SignalingManager
import com.callapp.server.service.InviteTokenParser
import com.callapp.server.service.InviteTokenService
import com.callapp.server.service.ManagementService
import com.callapp.server.service.MediaStorageService
import com.callapp.server.service.OnboardingService
import com.callapp.server.service.PasswordService
import com.callapp.server.service.TurnCredentialsService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import java.io.File
import javax.sql.DataSource

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val appConfig = AppConfig.from(environment.config)
    val databaseFactory = DatabaseFactory(appConfig.database)
    val dataSource = databaseFactory.createDataSource()
    val migrationRunner = MigrationRunner(dataSource)
    val jwtService = JwtService(appConfig.security)
    val passwordService = PasswordService()
    val inviteTokenParser = InviteTokenParser()
    val serverRepository = ServerRepository(dataSource)
    val userRepository = UserRepository(dataSource)
    val inviteTokenRepository = InviteTokenRepository(dataSource)
    val loginAttemptRepository = LoginAttemptRepository(dataSource)
    val joinRequestRepository = JoinRequestRepository(dataSource)
    val favoriteRepository = FavoriteRepository(dataSource)
    val notificationRepository = NotificationRepository(dataSource)
    val signalingManager = SignalingManager(serverRepository, userRepository, notificationRepository)
    val turnCredentialsService = TurnCredentialsService(appConfig.turn)
    val inviteTokenService = InviteTokenService(inviteTokenRepository, inviteTokenParser)
    val mediaStorageService = MediaStorageService(
        uploadRoot = File(appConfig.database.path).absoluteFile.parentFile.resolve("uploads").toPath(),
    )
    val onboardingService = OnboardingService(
        dataSource = dataSource,
        serverRepository = serverRepository,
        userRepository = userRepository,
        inviteTokenRepository = inviteTokenRepository,
        loginAttemptRepository = loginAttemptRepository,
        inviteTokenService = inviteTokenService,
        passwordService = passwordService,
        jwtService = jwtService,
    )
    val managementService = ManagementService(
        dataSource = dataSource,
        serverRepository = serverRepository,
        userRepository = userRepository,
        inviteTokenRepository = inviteTokenRepository,
        joinRequestRepository = joinRequestRepository,
        favoriteRepository = favoriteRepository,
        notificationRepository = notificationRepository,
        passwordService = passwordService,
    )

    migrationRunner.run()
    ServerBootstrap(dataSource, appConfig.server).ensureServerRow()
    BootstrapInviteTokenSeeder(
        inviteTokenRepository = inviteTokenRepository,
        serverConfig = appConfig.server,
        bootstrapConfig = appConfig.bootstrap,
    ).seedAdminInviteTokenIfConfigured()

    install(AppDependenciesPlugin) {
        config = appConfig
        database = dataSource
        this.jwtService = jwtService
        this.passwordService = passwordService
        this.inviteTokenParser = inviteTokenParser
        this.inviteTokenService = inviteTokenService
        this.onboardingService = onboardingService
        this.managementService = managementService
        this.mediaStorageService = mediaStorageService
        this.serverRepository = serverRepository
        this.userRepository = userRepository
        this.inviteTokenRepository = inviteTokenRepository
        this.loginAttemptRepository = loginAttemptRepository
        this.joinRequestRepository = joinRequestRepository
        this.favoriteRepository = favoriteRepository
        this.notificationRepository = notificationRepository
        this.signalingManager = signalingManager
        this.turnCredentialsService = turnCredentialsService
    }

    configureMonitoring()
    configureHTTP()
    configureSerialization()
    configureStatusPages()
    configureAuth()
    configureWebSockets()
    configureRouting()
}
