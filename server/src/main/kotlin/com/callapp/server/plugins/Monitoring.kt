package com.callapp.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()?.value ?: 0
            "${call.request.httpMethod.value} ${call.request.path()} -> $status"
        }
    }
}
