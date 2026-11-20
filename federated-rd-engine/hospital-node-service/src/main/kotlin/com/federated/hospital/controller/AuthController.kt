package com.federated.hospital.controller

import com.federated.hospital.config.JwtTokenProvider
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class LoginRequest(
    val username: String,
    val password: String
)

data class AuthTokenResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresInMs: Long = 86400000,
    val username: String
)

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider
) {

    private val validUsers = mapOf(
        "admin" to "password123",
        "doctor_a" to "doctor123",
        "doctor_b" to "doctor123",
        "doctor_c" to "doctor123"
    )

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        val expectedPassword = validUsers[request.username]
        if (expectedPassword != null && expectedPassword == request.password) {
            val token = jwtTokenProvider.generateToken(request.username)
            val response = AuthTokenResponse(
                token = token,
                username = request.username
            )
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            mapOf("error" to "Invalid username or password")
        )
    }
}
