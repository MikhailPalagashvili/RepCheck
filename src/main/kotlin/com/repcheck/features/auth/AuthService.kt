package com.repcheck.features.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class AuthService(private val jwt: JwtProvider = JwtProvider()) {

    data class Tokens(val accessToken: String, val refreshToken: String, val expiresIn: Long)

    fun login(email: String, password: String): Tokens = transaction {
        val normalized = email.trim().lowercase()
        val user = findByEmail(normalized) ?: throw IllegalArgumentException("Invalid credentials")

        val result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
        require(result.verified) { "Invalid credentials" }

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

    fun findByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .map {
                User(
                    id = it[Users.id].value,
                    email = it[Users.email],
                    passwordHash = it[Users.passwordHash],
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
                    createdAt = it[Users.createdAt]
                )
            }.singleOrNull()
    }

    fun register(email: String, password: String): User {
        val normalized = email.trim().lowercase()
        require(normalized.contains('@')) { "Invalid email" }
        require(password.length >= 8) { "Password must be at least 8 characters" }

        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())

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
            }

            User(
                id = userId,
                email = normalized,
                passwordHash = passwordHash,
                createdAt = Instant.now()
            )
        }
    }
}
