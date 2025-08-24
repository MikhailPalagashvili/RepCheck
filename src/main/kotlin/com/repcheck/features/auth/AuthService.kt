package com.repcheck.features.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.repcheck.db.Database
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class AuthService(private val jwt: JwtProvider = JwtProvider()) {

    data class User(val id: UUID, val email: String, val passwordHash: String, val createdAt: Instant)

    fun register(email: String, password: String): User {
        val normalized = email.trim().lowercase()
        require(normalized.contains('@')) { "Invalid email" }
        require(password.length >= 8) { "Password too short" }

        val existing = findByEmail(normalized)
        require(existing == null) { "Email already registered" }

        val id = UUID.randomUUID()
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val now = Instant.now()
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO users(id, email, password_hash, created_at) VALUES (?, ?, ?, ?)"
            ).use { ps ->
                ps.setObject(1, id)
                ps.setString(2, normalized)
                ps.setString(3, hash)
                ps.setTimestamp(4, Timestamp.from(now))
                ps.executeUpdate()
            }
        }
        return User(id, normalized, hash, now)
    }

    fun login(email: String, password: String): Pair<User, String> {
        val user = findByEmail(email.trim().lowercase()) ?: throw IllegalArgumentException("Invalid credentials")
        val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
        if (!verified) throw IllegalArgumentException("Invalid credentials")
        val token = jwt.createToken(user.id.toString())
        return user to token
    }

    fun findById(id: UUID): User? {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id, email, password_hash, created_at FROM users WHERE id = ?").use { ps ->
                ps.setObject(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        return User(
                            id = rs.getObject("id", UUID::class.java),
                            email = rs.getString("email"),
                            passwordHash = rs.getString("password_hash"),
                            createdAt = rs.getTimestamp("created_at").toInstant()
                        )
                    }
                }
            }
        }
        return null
    }

    private fun findByEmail(email: String): User? {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id, email, password_hash, created_at FROM users WHERE email = ?").use { ps ->
                ps.setString(1, email)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        return User(
                            id = rs.getObject("id", UUID::class.java),
                            email = rs.getString("email"),
                            passwordHash = rs.getString("password_hash"),
                            createdAt = rs.getTimestamp("created_at").toInstant()
                        )
                    }
                }
            }
        }
        return null
    }
}
