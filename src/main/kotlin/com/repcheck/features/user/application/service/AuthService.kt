package com.repcheck.features.user.application.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.repcheck.features.user.domain.model.User
import com.repcheck.features.user.domain.repository.UserRepository
import kotlinx.coroutines.*
import java.time.Instant
import java.util.*

class AuthService(
    private val userRepository: UserRepository,
    private val jwt: JwtProvider,
    private val emailService: EmailService = ConsoleEmailService(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : CoroutineScope, AutoCloseable {
    private val job = SupervisorJob()
    override val coroutineContext = coroutineScope.coroutineContext + job

    data class Tokens(val accessToken: String, val refreshToken: String, val expiresIn: Long)

    override fun close() {
        try {
            job.cancel("AuthService is shutting down")
            val parentJob = coroutineScope.coroutineContext[Job]
            if (parentJob != null && parentJob != job) {
                parentJob.cancel("AuthService parent scope is shutting down")
            }

            runBlocking {
                job.join()
                parentJob?.join()
            }
        } catch (e: Exception) {
            println("Error during AuthService shutdown: ${e.message}")
            throw e
        }
    }

    suspend fun login(email: String, password: String): Tokens {
        val normalized = email.trim().lowercase()
        val user = userRepository.findByEmail(normalized) ?: throw IllegalArgumentException("Invalid credentials")

        val result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
        require(result.verified) { "Invalid credentials" }
        require(user.isVerified) { "Please verify your email address before logging in" }

        return generateTokens(user.id)
    }

    private fun generateTokens(userId: UUID): Tokens {
        val accessToken = jwt.createToken(userId.toString())
        val refreshToken = UUID.randomUUID().toString()
        // TODO: Store refresh token in database with user association

        return Tokens(accessToken, refreshToken, jwt.expiresSeconds)
    }

    suspend fun verifyEmail(token: String): Boolean {
        return userRepository.verifyUser(token)
    }

    fun validateToken(token: String): UUID {
        return try {
            val decoded = jwt.verifier().verify(token)
            UUID.fromString(decoded.subject)
        } catch (e: Exception) {
            throw SecurityException("Invalid or expired token")
        }
    }

    suspend fun register(email: String, password: String): User {
        val normalized = email.trim().lowercase()
        require(normalized.contains('@')) { "Invalid email" }
        require(password.length >= 8) { "Password must be at least 8 characters" }

        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val verificationToken = UUID.randomUUID().toString()
        val tokenExpiresAt = Instant.now().plusSeconds(24 * 60 * 60) // 24 hours from now

        // Create user
        val user = userRepository.createUser(
            email = normalized,
            passwordHash = passwordHash
        )

        // Update with verification token
        userRepository.updateVerificationToken(user.id, verificationToken, tokenExpiresAt)

        // Send verification email
        coroutineScope.launch {
            try {
                emailService.sendVerificationEmail(
                    email = normalized,
                    token = verificationToken,
                    expiresAt = Date.from(tokenExpiresAt)
                )
            } catch (e: Exception) {
                // Log the error but don't fail the registration
                e.printStackTrace()
            }
        }

        return user.copy(verificationToken = verificationToken, verificationTokenExpiresAt = tokenExpiresAt)
    }
}

/**
 * Service for sending emails
 */
fun interface EmailService {
    /**
     * Sends a verification email to the specified address
     * @param email The recipient's email address
     * @param token The verification token to include in the email
     * @param expiresAt When the verification token expires
     */
    fun sendVerificationEmail(email: String, token: String, expiresAt: Date)
}

class ConsoleEmailService : EmailService {
    override fun sendVerificationEmail(email: String, token: String, expiresAt: Date) {
        println(
            """
            Sending verification email to: $email
            Verification token: $token
            Expires at: $expiresAt
            Verification link: http://localhost:8080/api/auth/verify?token=$token
        """.trimIndent()
        )
    }
}
