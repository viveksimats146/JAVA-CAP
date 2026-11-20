package com.federated.hospital.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${hospital.jwt-secret}") private val jwtSecret: String
) {

    private val secretKey: SecretKey by lazy {
        val keyBytes = jwtSecret.toByteArray(StandardCharsets.UTF_8)
        val paddedKey = if (keyBytes.size < 32) keyBytes.copyOf(32) else keyBytes
        Keys.hmacShaKeyFor(paddedKey)
    }

    private val expirationMs: Long = 86400000 // 24 hours

    fun generateToken(username: String, role: String = "USER"): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun getUsernameFromToken(token: String): String {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.subject
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}
