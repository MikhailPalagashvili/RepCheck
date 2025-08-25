package com.repcheck.testutils

import com.repcheck.features.user.application.service.AuthService
import com.repcheck.features.user.domain.repository.UserRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for integration tests.
 */
abstract class BaseTest : KoinTest {
    protected val authService: AuthService by inject()
    protected val userRepository: UserRepository by inject()

    @BeforeTest
    open fun setup() {
        TestDatabaseFactory.init()
    }

    @AfterTest
    open fun teardown() {
        TestDatabaseFactory.close()
    }

    /**
     * Use the Ktor test client provided by testApplication
     */
    internal fun ApplicationTestBuilder.jsonClient(): HttpClient =
        createClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }
        }

    @Serializable
    data class AuthResponse(val token: String)

    /**
     * Creates a user and returns JWT token
     */
    protected suspend fun ApplicationTestBuilder.createAndLoginUser(
        email: String = "test@example.com",
        password: String = "password123"
    ): String {
        val client = jsonClient()

        // Register
        client.post("/api/v1/auth/register") {
            setBody("""{ "email": "$email", "password": "$password" }""")
        }

        // Login
        val response = client.post("/api/v1/auth/login") {
            setBody("""{ "email": "$email", "password": "$password" }""")
        }

        // Deserialize JSON token instead of relying on headers
        val authResponse: AuthResponse = response.body()
        return authResponse.token
    }
}
