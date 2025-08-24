package com.repcheck.features.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

@kotlinx.serialization.Serializable
data class RegisterRequest(val email: String, val password: String)

@kotlinx.serialization.Serializable
data class LoginRequest(val email: String, val password: String)

@kotlinx.serialization.Serializable
data class TokenResponse(
    val accessToken: String, val tokenType: String = "Bearer", val expiresIn: Long
)

@kotlinx.serialization.Serializable
data class UserResponse(
    val id: String, val email: String, val createdAt: String
)

fun Route.authRoutes(authService: AuthService) {
    route("/api/v1/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            try {
                val user = authService.register(req.email, req.password)
                call.respond(
                    HttpStatusCode.Created, UserResponse(user.id.toString(), user.email, user.createdAt.toString())
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
                    HttpStatusCode.OK, TokenResponse(
                        accessToken = tokens.accessToken, tokenType = "Bearer", expiresIn = tokens.expiresIn
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.Unauthorized, mapOf<String, String>("error" to (e.message ?: "Invalid credentials"))
                )
            }
        }
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.subject ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val user =
                    authService.findById(UUID.fromString(userId)) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(
                    HttpStatusCode.OK, UserResponse(
                        id = user.id.toString(), email = user.email, createdAt = user.createdAt.toString()
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
