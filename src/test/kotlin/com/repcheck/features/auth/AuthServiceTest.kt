package com.repcheck.features.auth

import com.repcheck.config.DatabaseConfig
import com.repcheck.db.DatabaseFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthServiceTest {
    private lateinit var authService: AuthService
    private val testDbConfig = DatabaseConfig(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "sa",
        password = "",
        driver = "org.h2.Driver",
        maxPoolSize = 1
    )

    @BeforeEach
    fun setup() {
        // Initialize test database
        DatabaseFactory.init(testDbConfig)

        // Create tables
        transaction {
            SchemaUtils.create(Users)
        }

        authService = AuthService()
    }

    @AfterEach
    fun tearDown() {
        // Drop tables
        transaction {
            SchemaUtils.drop(Users)
        }
    }

    @Test
    fun `register and login user successfully`() = runTest {
        // Register a new user
        val email = "test@example.com"
        val password = "password123"
        val user = authService.register(email, password)

        // Verify user was created
        assertEquals(email, user.email)
        assertNotNull(user.id)
        assertNotNull(user.createdAt)

        // Test login with correct credentials
        val tokens = authService.login(email, password)
        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)
        assert(tokens.expiresIn > 0)

        // Test login with wrong password
        assertFailsWith<IllegalArgumentException> {
            authService.login(email, "wrongpassword")
        }

        // Test login with non-existent user
        assertFailsWith<IllegalArgumentException> {
            authService.login("nonexistent@example.com", password)
        }
    }

    @Test
    fun `find user by id`() = runTest {
        // Register a user
        val user = authService.register("test@example.com", "password123")

        // Find the user by ID
        val foundUser = authService.findById(user.id)
        assertNotNull(foundUser)
        assertEquals(user.id, foundUser?.id)
        assertEquals(user.email, foundUser?.email)

        // Try to find non-existent user
        val nonExistentUser = authService.findById(UUID.randomUUID())
        assertNull(nonExistentUser)
    }
}
