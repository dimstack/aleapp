package com.callapp.server.service

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordServiceTest {

    private val service = PasswordService()

    @Test
    fun hashPassword() {
        val hash = service.hash("password123")

        assertNotEquals("password123", hash)
    }

    @Test
    fun hashProducesBcryptPrefix() {
        val hash = service.hash("password123")

        assertTrue(Regex("""^\$2[aby]?\$12\$.*""").matches(hash))
    }

    @Test
    fun verifyCorrectPassword() {
        val hash = service.hash("password123")

        assertTrue(service.verify("password123", hash))
    }

    @Test
    fun verifyWrongPassword() {
        val hash = service.hash("password123")

        assertFalse(service.verify("wrong-password", hash))
    }

    @Test
    fun hashSamePasswordTwice() {
        val firstHash = service.hash("password123")
        val secondHash = service.hash("password123")

        assertNotEquals(firstHash, secondHash)
    }

    @Test
    fun shortPassword() {
        val hash = service.hash("short")

        assertTrue(service.verify("short", hash))
    }

    @Test
    fun unicodePassword_roundTrip() {
        val hash = service.hash("\u043F\u0430\u0440\u043E\u043B\u044C-\u79D8\u5BC6")

        assertTrue(service.verify("\u043F\u0430\u0440\u043E\u043B\u044C-\u79D8\u5BC6", hash))
    }

    @Test
    fun emptyPassword_roundTrip_ifValidationIsOutsideService() {
        val hash = service.hash("")

        assertTrue(service.verify("", hash))
    }

    @Test
    fun verifyInvalidHashFormat_behaviorIsDefined() {
        assertFailsWith<IllegalArgumentException> {
            service.verify("password123", "not-a-bcrypt-hash")
        }
    }
}
