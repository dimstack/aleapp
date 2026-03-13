package com.callapp.server

import com.callapp.server.config.AppConfig
import javax.sql.DataSource
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.util.AttributeKey

data class AppDependencies(
    val config: AppConfig,
    val database: DataSource,
)

class AppDependenciesPluginConfig {
    lateinit var config: AppConfig
    lateinit var database: DataSource
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
        ),
    )
}

val Application.dependencies: AppDependencies
    get() = attributes[AppDependenciesKey]
