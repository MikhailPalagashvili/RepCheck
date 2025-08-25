package com.repcheck.features.user.domain.repository

import com.repcheck.features.user.domain.model.User
import java.util.*

interface UserRepository {
    suspend fun createUser(email: String, passwordHash: String): User
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun updateUser(user: User): Boolean
    suspend fun deleteUser(id: UUID): Boolean
    suspend fun updateVerificationToken(userId: UUID, token: String, expiresAt: java.time.Instant)
    suspend fun verifyUser(token: String): Boolean
}
