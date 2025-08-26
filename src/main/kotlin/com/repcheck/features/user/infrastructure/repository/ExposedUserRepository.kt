package com.repcheck.features.user.infrastructure.repository

import com.repcheck.features.user.domain.model.User
import com.repcheck.features.user.domain.repository.UserRepository
import com.repcheck.features.user.infrastructure.table.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.*

class ExposedUserRepository : UserRepository {
    override suspend fun createUser(email: String, passwordHash: String): User = newSuspendedTransaction {
        Users.insert {
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.createdAt] = Instant.now()
            it[Users.isVerified] = false
        }.let { result ->
            User(
                id = result[Users.id].value,
                email = result[Users.email],
                passwordHash = result[Users.passwordHash],
                isVerified = result[Users.isVerified],
                verificationToken = result[Users.verificationToken],
                verificationTokenExpiresAt = result[Users.verificationTokenExpiresAt],
                createdAt = result[Users.createdAt]
            )
        }
    }

    override suspend fun findByEmail(email: String): User? = newSuspendedTransaction {
        Users.select { Users.email eq email }
            .map { row ->
                User(
                    id = row[Users.id].value,
                    email = row[Users.email],
                    passwordHash = row[Users.passwordHash],
                    isVerified = row[Users.isVerified],
                    verificationToken = row[Users.verificationToken],
                    verificationTokenExpiresAt = row[Users.verificationTokenExpiresAt],
                    createdAt = row[Users.createdAt]
                )
            }.singleOrNull()
    }

    override suspend fun updateVerificationToken(userId: UUID, token: String, expiresAt: Instant) {
        newSuspendedTransaction {
            Users.update({ Users.id eq userId }) {
                it[Users.verificationToken] = token
                it[Users.verificationTokenExpiresAt] = expiresAt
            }
        }
    }

    override suspend fun verifyUser(token: String): Boolean = newSuspendedTransaction {
        val now = Instant.now()
        val result = Users.update(
            where = {
                (Users.verificationToken eq token) and
                        (Users.verificationTokenExpiresAt greater now)
            }
        ) {
            it[isVerified] = true
            it[verificationToken] = null
            it[verificationTokenExpiresAt] = null
        }
        result > 0
    }

    override suspend fun findById(id: UUID): User? = newSuspendedTransaction {
        Users.select { Users.id eq id }
            .map { row ->
                User(
                    id = row[Users.id].value,
                    email = row[Users.email],
                    passwordHash = row[Users.passwordHash],
                    isVerified = row[Users.isVerified],
                    verificationToken = row[Users.verificationToken],
                    verificationTokenExpiresAt = row[Users.verificationTokenExpiresAt],
                    createdAt = row[Users.createdAt]
                )
            }.singleOrNull()
    }

    override suspend fun updateUser(user: User): Boolean = newSuspendedTransaction {
        val updatedRows = Users.update({ Users.id eq user.id }) {
            it[Users.email] = user.email
            it[Users.passwordHash] = user.passwordHash
            it[Users.isVerified] = user.isVerified
            it[Users.verificationToken] = user.verificationToken
            it[Users.verificationTokenExpiresAt] = user.verificationTokenExpiresAt
        }
        updatedRows > 0
    }

    override suspend fun deleteUser(id: UUID): Boolean = newSuspendedTransaction {
        val deletedRows = Users.deleteWhere { Users.id eq id }
        deletedRows > 0
    }
}
