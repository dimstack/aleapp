package com.callapp.server

import com.callapp.server.config.AppConfig
import com.callapp.server.database.DatabaseFactory
import com.callapp.server.database.MigrationRunner
import com.callapp.server.database.ServerBootstrap
import com.callapp.server.plugins.configureHTTP
import com.callapp.server.plugins.configureMonitoring
import com.callapp.server.plugins.configureRouting
import com.callapp.server.plugins.configureSerialization
import com.callapp.server.plugins.configureStatusPages
import com.callapp.server.plugins.configureWebSockets
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val appConfig = AppConfig.from(environment.config)
    val databaseFactory = DatabaseFactory(appConfig.database)
    val dataSource = databaseFactory.createDataSource()
    val migrationRunner = MigrationRunner(dataSource)

    migrationRunner.run()
    ServerBootstrap(dataSource, appConfig.server).ensureServerRow()

    install(AppDependenciesPlugin) {
        config = appConfig
        database = dataSource
    }

    configureMonitoring()
    configureHTTP()
    configureSerialization()
    configureStatusPages()
    configureWebSockets()
    configureRouting()
}
