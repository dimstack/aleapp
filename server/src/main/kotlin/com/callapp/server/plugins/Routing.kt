package com.callapp.server.plugins

import com.callapp.server.database.HealthRepository
import com.callapp.server.dependencies
import com.callapp.server.routes.HealthResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "service" to "callapp-server",
                    "status" to "ok",
                ),
            )
        }

        get("/health") {
            val databaseState = HealthRepository(this@configureRouting.dependencies.database).probe()
            call.respond(
                HttpStatusCode.OK,
                HealthResponse(
                    service = "callapp-server",
                    environment = this@configureRouting.dependencies.config.environment,
                    status = if (databaseState.connected) "ok" else "degraded",
                    database = databaseState,
                ),
            )
        }
    }
}
