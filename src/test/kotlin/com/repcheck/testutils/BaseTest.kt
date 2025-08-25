package com.repcheck.testutils

import com.repcheck.di.appModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Test user credentials for authentication tests
 */
data class TestUser(
    val email: String = "test@example.com",
    val password: String = "testpassword123"
)

/**
 * Base test class for all integration tests.
 * Provides common test utilities and configurations.
 */
abstract class BaseTest : KoinTest {

    @BeforeTest
    fun setup() {
        startKoin {
            modules(appModule)
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    /**
     * Creates a test HTTP client with JSON support
     */
    protected fun createTestClient(
        baseUrl: String = "http://localhost",
        token: String? = null,
        handleRequest: HttpRequestBuilder.() -> Unit = {}
    ): HttpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }

        expectSuccess = false // We'll handle success/failure in tests

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)

            // Add authorization header if token is provided
            token?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }

    /**
     * Creates and logs in a test user, returning the JWT token
     */
    protected suspend fun createAndLoginUser(
        user: TestUser = TestUser(),
        client: HttpClient
    ): String {
        // Register test user
        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "email": "${user.email}",
                    "password": "${user.password}"
                }
            """.trimIndent()
            )
        }

        // Login and get token
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "email": "${user.email}",
                    "password": "${user.password}"
                }
            """.trimIndent()
            )
        }

        // Extract and return the token
        return response.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: ""
    }

    /**
     * Creates an authenticated client with a JWT token
     */
    protected fun createAuthenticatedClient(
        token: String,
        baseUrl: String = "http://localhost",
        handleRequest: HttpRequestBuilder.() -> Unit = {}
    ): HttpClient = createTestClient(baseUrl, token, handleRequest)
}