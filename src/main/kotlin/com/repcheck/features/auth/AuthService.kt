package com.repcheck.features.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class AuthService(
    private val jwt: JwtProvider = JwtProvider(),
    private val emailService: EmailService = ConsoleEmailService()
) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.Default + job

    fun close() {
        job.cancel()
    }

    data class Tokens(val accessToken: String, val refreshToken: String, val expiresIn: Long)

    fun login(email: String, password: String): Tokens = transaction {
        val normalized = email.trim().lowercase()
        val user = findByEmail(normalized) ?: throw IllegalArgumentException("Invalid credentials")

        val result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
        require(result.verified) { "Invalid credentials" }
        require(user.isVerified) { "Please verify your email address before logging in" }

        generateTokens(user.id)
    }

    private fun generateTokens(userId: UUID): Tokens {
        val accessToken = jwt.createToken(userId.toString())
        val refreshToken = UUID.randomUUID().toString()
        // TODO: Store refresh token in database with user association

        return Tokens(accessToken, refreshToken, jwt.expiresSeconds)
    }

    fun validateToken(token: String): UUID {
        return try {
            val decoded = jwt.verifier().verify(token)
            UUID.fromString(decoded.subject)
        } catch (e: Exception) {
            throw SecurityException("Invalid or expired token")
        }
    }

    fun verifyEmail(token: String): Boolean = transaction {
        val now = Instant.now()

        val user = Users.select {
            (Users.verificationToken eq token) and
                    (Users.verificationTokenExpiresAt greater now)
        }.map {
            it[Users.id].value
        }.singleOrNull() ?: return@transaction false

        Users.update({ Users.id eq user }) {
            it[isVerified] = true
            it[verificationToken] = null
            it[verificationTokenExpiresAt] = null
        } > 0
    }

    fun findByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .map {
                User(
                    id = it[Users.id].value,
                    email = it[Users.email],
                    passwordHash = it[Users.passwordHash],
                    isVerified = it[Users.isVerified],
                    verificationToken = it[Users.verificationToken],
                    verificationTokenExpiresAt = it[Users.verificationTokenExpiresAt],
                    createdAt = it[Users.createdAt]
                )
            }.singleOrNull()
    }

    fun findById(id: UUID): User? = transaction {
        Users.select { Users.id eq id }
            .map {
                User(
                    id = it[Users.id].value,
                    email = it[Users.email],
                    passwordHash = it[Users.passwordHash],
                    isVerified = it[Users.isVerified],
                    verificationToken = it[Users.verificationToken],
                    verificationTokenExpiresAt = it[Users.verificationTokenExpiresAt],
                    createdAt = it[Users.createdAt]
                )
            }.singleOrNull()
    }

    suspend fun register(email: String, password: String): User {
        val normalized = email.trim().lowercase()
        require(normalized.contains('@')) { "Invalid email" }
        require(password.length >= 8) { "Password must be at least 8 characters" }

        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val verificationToken = UUID.randomUUID().toString()
        val tokenExpiresAt = Instant.now().plusSeconds(24 * 60 * 60) // 24 hours from now

        return transaction {
            // Check if user already exists
            if (Users.select { Users.email eq normalized }.count() > 0) {
                throw IllegalArgumentException("User with this email already exists")
            }

            val userId = UUID.randomUUID()
            Users.insert {
                it[Users.id] = userId
                it[Users.email] = normalized
                it[Users.passwordHash] = passwordHash
                it[Users.verificationToken] = verificationToken
                it[Users.verificationTokenExpiresAt] = tokenExpiresAt
            }

            // Return the user first to complete the transaction
            val user = User(
                id = userId,
                email = normalized,
                passwordHash = passwordHash,
                isVerified = false,
                verificationToken = verificationToken,
                verificationTokenExpiresAt = tokenExpiresAt,
                createdAt = Instant.now()
            )

            // Send verification email after the transaction is complete
            launch(Dispatchers.IO) {
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

            user
        }
    }
}
