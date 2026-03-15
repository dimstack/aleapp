package com.callapp.android.network

import java.net.URI

data class ParsedInviteToken(
    val host: String,
    val port: Int,
    val code: String,
) {
    val serverAddress: String = "http://$host:$port"
}

object InviteTokenParser {
    private const val DEFAULT_PORT = 3000

    fun parse(rawToken: String): ParsedInviteToken? {
        val trimmed = rawToken.trim()
        if (trimmed.isBlank()) return null

        val sanitized = trimmed.removePrefix("https://").removePrefix("http://")
        val lastSlash = sanitized.lastIndexOf('/')
        if (lastSlash <= 0 || lastSlash == sanitized.lastIndex) return null

        val addressPart = sanitized.substring(0, lastSlash).trim()
        val code = sanitized.substring(lastSlash + 1).trim()
        if (addressPart.isBlank() || code.isBlank()) return null

        val uri = runCatching { URI("http://$addressPart") }.getOrNull() ?: return null
        val host = uri.host?.trim().orEmpty()
        if (host.isBlank()) return null

        return ParsedInviteToken(
            host = host,
            port = if (uri.port == -1) DEFAULT_PORT else uri.port,
            code = code,
        )
    }
}
