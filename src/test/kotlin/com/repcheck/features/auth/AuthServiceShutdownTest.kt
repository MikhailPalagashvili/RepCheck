package com.repcheck.features.auth

import com.repcheck.config.DatabaseConfig
import com.repcheck.db.DatabaseFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class AuthServiceShutdownTest {
    private val testDbConfig = DatabaseConfig(
        url = "jdbc:h2:mem:test-shutdown;DB_CLOSE_DELAY=-1",
        user = "sa",
        password = "",
        driver = "org.h2.Driver",
        maxPoolSize = 1
    )

    @BeforeEach
    fun setup() {
        DatabaseFactory.init(testDbConfig)
        transaction {
            SchemaUtils.create(Users)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Users)
        }
    }

    @Test
    fun `close should not throw when called multiple times`() = runTest {
        // Given
        val jwt = JwtProvider()
        val service = AuthService(jwt)
        
        // When/Then
        service.close()
        service.close() // Should not throw on second call
    }

    @Test
    fun `close should cancel running coroutines`() = runTest {
        // Given
        val jwt = JwtProvider()
        val service = AuthService(jwt)
        
        // Launch a coroutine in the service's scope that should be cancelled by service.close()
        var wasCancelled = false
        val job = service.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                delay(1000) // This will be cancelled
                wasCancelled = false // Shouldn't reach here
            } catch (e: CancellationException) {
                wasCancelled = true
                throw e
            }
        }

        // When
        service.close()
        
        // Give some time for cancellation to propagate
        delay(100)

        // Then
        assertTrue(wasCancelled, "Coroutine should be cancelled")
        assertTrue(job.isCancelled, "Job should be cancelled")
    }
}
