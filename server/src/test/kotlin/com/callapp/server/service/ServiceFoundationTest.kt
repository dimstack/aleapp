package com.callapp.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceFoundationTest {

    @Test
    fun passwordServiceHashesAndVerifiesPassword() {
        val passwordService = PasswordService()
        val hash = passwordService.hash("strong-password")

        assertTrue(passwordService.verify("strong-password", hash))
    }

    @Test
    fun inviteTokenParserAcceptsFullTokenOrCodeOnly() {
        val parser = InviteTokenParser()

        assertEquals("ABC12345", parser.extractCode("https://server.example.com:3000/ABC12345"))
        assertEquals("ABC12345", parser.extractCode("ABC12345"))
    }
}
