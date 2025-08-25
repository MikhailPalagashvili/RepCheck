package com.repcheck.features.user.domain.model

import java.time.Instant
import java.util.*

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val isVerified: Boolean = false,
    val verificationToken: String? = null,
    val verificationTokenExpiresAt: Instant? = null,
    val createdAt: Instant
)
