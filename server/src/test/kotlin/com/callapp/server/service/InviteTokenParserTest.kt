package com.callapp.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InviteTokenParserTest {

    private val parser = InviteTokenParser()

    @Test
    fun parseFullToken() {
        val parsed = parser.parse("host:3000/CODE")

        requireNotNull(parsed)
        assertEquals("host", parsed.serverAddress.host)
        assertEquals(3000, parsed.serverAddress.port)
        assertEquals("CODE", parsed.code)
    }

    @Test
    fun parseTokenWithoutPort() {
        val parsed = parser.parse("host/CODE")

        requireNotNull(parsed)
        assertEquals("host", parsed.serverAddress.host)
        assertEquals(3000, parsed.serverAddress.port)
        assertEquals("CODE", parsed.code)
    }

    @Test
    fun parseTokenWithScheme() {
        val parsed = parser.parse("https://host/CODE")

        requireNotNull(parsed)
        assertEquals("host", parsed.serverAddress.host)
        assertEquals(3000, parsed.serverAddress.port)
        assertEquals("CODE", parsed.code)
    }

    @Test
    fun parseInvalidFormat() {
        assertNull(parser.parse("host:3000/"))
    }

    @Test
    fun parseCodeOnly() {
        assertNull(parser.parse("CODE"))
    }
}
