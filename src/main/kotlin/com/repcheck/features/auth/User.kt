package com.repcheck.features.auth

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val isVerified = bool("is_verified").default(false)
    val verificationToken = varchar("verification_token", 255).nullable()
    val verificationTokenExpiresAt = timestamp("verification_token_expires_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val isVerified: Boolean = false,
    val verificationToken: String? = null,
    val verificationTokenExpiresAt: Instant? = null,
    val createdAt: Instant
)
