package com.repcheck.testutils

import com.repcheck.config.AppConfig
import com.repcheck.db.Database
import com.repcheck.di.appModule
import com.repcheck.features.user.application.service.AuthService
import com.repcheck.features.user.domain.repository.UserRepository
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class BaseTest : KoinTest {
    protected val testDbUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
    protected val testDbUser = "sa"
    protected val testDbPassword = ""

    protected val authService: AuthService by inject()
    protected val userRepository: UserRepository by inject()

    @BeforeTest
    open fun setup() {
        // Start Koin with test modules
        startKoin {
            modules(appModule)
        }

        // Initialize test database
        Database.init(
            AppConfig.DatabaseConfig(
                url = testDbUrl,
                user = testDbUser,
                password = testDbPassword
            )
        )

        // Run migrations
        runMigrations()
    }

    @AfterTest
    open fun teardown() {
        stopKoin()
    }

    private fun runMigrations() {
        val flyway = org.flywaydb.core.Flyway.configure()
            .dataSource(testDbUrl, testDbUser, testDbPassword)
            .load()
        flyway.migrate()
    }

    protected fun testApplication(
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = io.ktor.server.testing.testApplication {
        environment {
            config = org.koin.java.KoinJavaComponent.getKoin().getProperty<ApplicationConfig>("app.config")
        }
        application {
            // Your application module function here
        }
        runBlocking {
            block()
        }
    }

    protected fun createTestClient() = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }

    protected suspend fun createTestUser(
        email: String = "test@example.com",
        password: String = "password123"
    ) = authService.register(email, password)
}
