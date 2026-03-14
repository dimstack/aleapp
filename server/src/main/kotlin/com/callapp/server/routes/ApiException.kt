package com.callapp.server.routes

import io.ktor.http.HttpStatusCode

class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
    val details: String? = null,
) : RuntimeException(message)
