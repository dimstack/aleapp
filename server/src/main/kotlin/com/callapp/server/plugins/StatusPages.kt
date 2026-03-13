package com.callapp.server.plugins

import com.callapp.server.routes.ApiException
import com.callapp.server.routes.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(
                cause.status,
                ErrorResponse(
                    code = cause.code,
                    message = cause.message,
                    details = cause.details,
                ),
            )
        }
        exception<Throwable> { call, cause ->
            this@configureStatusPages.environment.log.error("Unhandled server error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = "server_error",
                    message = cause.message ?: "Internal server error",
                ),
            )
        }
    }
}
