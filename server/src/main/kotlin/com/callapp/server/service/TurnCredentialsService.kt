package com.callapp.server.service

import com.callapp.server.config.TurnConfig
import com.callapp.server.routes.TurnCredentialsDto
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TurnCredentialsService(
    private val config: TurnConfig,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun create(userId: String): TurnCredentialsDto {
        val expiresAt = Instant.now(clock).epochSecond + config.ttlSeconds
        val username = "$expiresAt:$userId"
        val credential = hmacSha1Base64(username, config.secret)
        return TurnCredentialsDto(
            urls = listOf(
                "turn:${config.host}:${config.port}?transport=udp",
                "turn:${config.host}:${config.port}?transport=tcp",
            ),
            username = username,
            credential = credential,
        )
    }

    private fun hmacSha1Base64(value: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA1")
        mac.init(keySpec)
        return Base64.getEncoder().encodeToString(mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }
}
