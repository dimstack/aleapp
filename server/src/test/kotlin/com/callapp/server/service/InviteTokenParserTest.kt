package com.callapp.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun parseTokenWithIpAddress() {
        val parsed = parser.parse("192.168.1.1:3000/CODE")

        requireNotNull(parsed)
        assertEquals("192.168.1.1", parsed.serverAddress.host)
        assertEquals(3000, parsed.serverAddress.port)
        assertEquals("CODE", parsed.code)
    }

    @Test
    fun parseTokenWithLocalhost() {
        val parsed = parser.parse("localhost:3000/CODE")

        requireNotNull(parsed)
        assertEquals("localhost", parsed.serverAddress.host)
        assertEquals(3000, parsed.serverAddress.port)
        assertEquals("CODE", parsed.code)
    }

    @Test
    fun parseTokenWithWhitespace_trimsCorrectly() {
        val parsed = parser.parse("  host:3001/CODE  ")

        requireNotNull(parsed)
        assertEquals("host", parsed.serverAddress.host)
        assertEquals(3001, parsed.serverAddress.port)
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

    @Test
    fun parseBlankToken_returnsNull() {
        assertNull(parser.parse("   "))
    }

    @Test
    fun parseMissingHost_returnsNull() {
        assertNull(parser.parse(":3000/CODE"))
    }

    @Test
    fun parseInvalidPort_returnsNull() {
        assertNull(parser.parse("host:abc/CODE"))
    }

    @Test
    fun extractCode_fromFullToken() {
        assertEquals("CODE", parser.extractCode("host:3000/CODE"))
    }

    @Test
    fun extractCode_fromCodeOnly() {
        assertEquals("CODE", parser.extractCode("CODE"))
    }

    @Test
    fun extractCode_blank_throws() {
        assertFailsWith<IllegalArgumentException> {
            parser.extractCode("   ")
        }
    }

    @Test
    fun extractCode_trailingSlash_throws() {
        assertFailsWith<IllegalArgumentException> {
            parser.extractCode("host:3000/")
        }
    }
}
