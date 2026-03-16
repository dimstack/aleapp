package com.callapp.android.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenParserTest {

    @Test
    fun parseValidToken() {
        val parsed = InviteTokenParser.parse("server.example.com:3000/ABCD1234")

        requireNotNull(parsed)
        assertEquals("server.example.com", parsed.host)
        assertEquals(3000, parsed.port)
        assertEquals("ABCD1234", parsed.code)
    }

    @Test
    fun parseTokenWithoutPort() {
        val parsed = InviteTokenParser.parse("server.example.com/ABCD1234")

        requireNotNull(parsed)
        assertEquals("server.example.com", parsed.host)
        assertEquals(3000, parsed.port)
        assertEquals("ABCD1234", parsed.code)
    }

    @Test
    fun parseTokenWithHttpsScheme() {
        val parsed = InviteTokenParser.parse("https://server.example.com/ABCD1234")

        requireNotNull(parsed)
        assertEquals("server.example.com", parsed.host)
        assertEquals(3000, parsed.port)
        assertEquals("ABCD1234", parsed.code)
    }

    @Test
    fun parseTokenWithHttpScheme() {
        val parsed = InviteTokenParser.parse("http://server.example.com/ABCD1234")

        requireNotNull(parsed)
        assertEquals("server.example.com", parsed.host)
        assertEquals(3000, parsed.port)
        assertEquals("ABCD1234", parsed.code)
    }

    @Test
    fun parseTokenWithLeadingTrailingWhitespace() {
        val parsed = InviteTokenParser.parse("  server.example.com:3000/ABCD1234  ")

        requireNotNull(parsed)
        assertEquals("server.example.com", parsed.host)
        assertEquals(3000, parsed.port)
        assertEquals("ABCD1234", parsed.code)
    }

    @Test
    fun parseTokenMissingCode() {
        assertNull(InviteTokenParser.parse("server.example.com:3000/"))
    }

    @Test
    fun parseEmptyToken() {
        assertNull(InviteTokenParser.parse(""))
    }

    @Test
    fun parseMalformedPort_returnsNull() {
        assertNull(InviteTokenParser.parse("server.example.com:abc/ABCD1234"))
    }

    @Test
    fun parseMissingHost_returnsNull() {
        assertNull(InviteTokenParser.parse(":3000/ABCD1234"))
    }

    @Test
    fun parseOnlyCode_returnsNull() {
        assertNull(InviteTokenParser.parse("ABCD1234"))
    }

    @Test
    fun parseTokenWithIpAddress() {
        val parsed = InviteTokenParser.parse("192.168.1.1:3000/TOKEN1")

        requireNotNull(parsed)
        assertEquals("192.168.1.1", parsed.host)
        assertEquals(3000, parsed.port)
        assertEquals("TOKEN1", parsed.code)
    }

    @Test
    fun parseTokenWithLocalhostAddress() {
        val parsed = InviteTokenParser.parse("localhost:3000/TOKEN1")

        requireNotNull(parsed)
        assertEquals("localhost", parsed.host)
        assertEquals(3000, parsed.port)
        assertEquals("TOKEN1", parsed.code)
    }
}
