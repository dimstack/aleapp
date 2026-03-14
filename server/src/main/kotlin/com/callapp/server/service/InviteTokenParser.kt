package com.callapp.server.service

class InviteTokenParser {
    fun extractCode(rawToken: String): String {
        val trimmed = rawToken.trim()
        require(trimmed.isNotBlank()) { "Invite token is blank" }
        val slashIndex = trimmed.lastIndexOf('/')
        return if (slashIndex >= 0) {
            trimmed.substring(slashIndex + 1).trim().also {
                require(it.isNotBlank()) { "Invite token code is blank" }
            }
        } else {
            trimmed
        }
    }
}
