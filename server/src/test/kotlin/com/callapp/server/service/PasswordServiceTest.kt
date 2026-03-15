package com.callapp.server.service

import kotlin.test.Test
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
}
