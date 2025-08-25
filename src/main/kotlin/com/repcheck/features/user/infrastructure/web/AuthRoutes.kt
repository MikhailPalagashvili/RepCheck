package com.repcheck.features.user.infrastructure.web

import com.repcheck.features.user.application.service.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val isVerified: Boolean,
    val createdAt: String
)

fun Route.authRoutes(authService: AuthService) {
    route("/api/v1/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            try {
                val user = authService.register(req.email, req.password)
                call.respond(
                    HttpStatusCode.Created,
                    UserResponse(
                        id = user.id.toString(),
                        email = user.email,
                        isVerified = user.isVerified,
                        createdAt = user.createdAt.toString()
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            }
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            try {
                val tokens = authService.login(req.email, req.password)
                call.respond(
                    HttpStatusCode.OK,
                    TokenResponse(
                        accessToken = tokens.accessToken,
                        refreshToken = tokens.refreshToken,
                        tokenType = "Bearer",
                        expiresIn = tokens.expiresIn
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to (e.message ?: "Invalid credentials"))
                )
            }
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.subject ?: return@get call.respond(HttpStatusCode.Unauthorized)

                // In a real app, you might want to fetch the user from the database
                // For now, we'll just return the user ID from the token
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "userId" to userId,
                        "message" to "This is a protected endpoint. Your user ID is: $userId"
                    )
                )
            }
        }

        // Email verification endpoint
        get("/verify-email") {
            val token = call.request.queryParameters["token"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing token"))

            val isVerified = authService.verifyEmail(token)
            if (isVerified) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Email verified successfully"))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid or expired verification token"))
            }
        }
    }
}
