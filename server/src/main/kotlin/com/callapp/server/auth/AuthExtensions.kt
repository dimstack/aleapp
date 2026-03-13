package com.callapp.server.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

fun ApplicationCall.sessionPrincipal(): SessionPrincipal? = principal()

fun ApplicationCall.requireSessionPrincipal(): SessionPrincipal =
    checkNotNull(sessionPrincipal()) { "Authenticated principal is missing" }
