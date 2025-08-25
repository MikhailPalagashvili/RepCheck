package com.repcheck.features.user.application

import com.repcheck.testutils.BaseTest
import com.repcheck.testutils.TestUser
import io.kotest.common.runBlocking
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val email: String,
    val isVerified: Boolean
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

class AuthFlowTest : BaseTest() {
    private val testUser = TestUser("test@example.com", "password123")

    @Test
    fun `test user registration and login flow`() = testApplication {
        val client = createTestClient()

        // 1. Register a new user
        val registerRequest = RegisterRequest(
            email = testUser.email,
            password = testUser.password,
            firstName = "Test",
            lastName = "User"
        )

        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(registerRequest))
        }

        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val registerResponseBody = Json.decodeFromString<Map<String, Any>>(registerResponse.bodyAsText())
        assertEquals("test@example.com", registerResponseBody["email"])
        assertEquals(false, registerResponseBody["isVerified"])

        // 2. Try to login before verification (should fail)
        val loginRequest = LoginRequest(
            email = testUser.email,
            password = testUser.password
        )

        val loginBeforeVerifyResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(loginRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, loginBeforeVerifyResponse.status)
        val loginError = Json.decodeFromString<Map<String, String>>(loginBeforeVerifyResponse.bodyAsText())
        assertEquals("Email not verified", loginError["message"])

        // 3. Verify the user's email (mocking the verification token for testing)
        val verificationToken = "test-verification-token"
        val verifyResponse = client.get("/api/v1/auth/verify-email?token=$verificationToken")
        assertEquals(HttpStatusCode.OK, verifyResponse.status)
        val verifyResponseBody = Json.decodeFromString<Map<String, Any>>(verifyResponse.bodyAsText())
        assertEquals(true, verifyResponseBody["success"])

        // 4. Login after verification (should succeed)
        val loginResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(loginRequest))
        }

        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginResponseBody = Json.decodeFromString<Map<String, Any>>(loginResponse.bodyAsText())
        assertNotNull(loginResponseBody["token"])
        assertNotNull(loginResponseBody["refreshToken"])
        assertEquals("test@example.com", loginResponseBody["email"])
        assertEquals(true, loginResponseBody["isVerified"])

        // 5. Test token refresh
        val refreshToken = loginResponseBody["refreshToken"] as String
        val refreshRequest = RefreshTokenRequest(refreshToken)
        val refreshResponse = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(refreshRequest))
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val refreshResponseBody = Json.decodeFromString<Map<String, String>>(refreshResponse.bodyAsText())
        assertNotNull(refreshResponseBody["token"])
        assertNotNull(refreshResponseBody["refreshToken"])

        // Check for either token or accessToken/refreshToken based on your auth implementation
        if (loginResponseBody.containsKey("token")) {
            val authToken = loginResponseBody["token"] as String
            // Test accessing a protected endpoint with token
            val protectedResponse = client.get("/api/v1/user/me") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }

            assertEquals(HttpStatusCode.OK, protectedResponse.status)
            val userInfo = Json.decodeFromString<Map<String, Any>>(protectedResponse.bodyAsText())
            assertEquals("test@example.com", userInfo["email"])
            assertEquals(true, userInfo["isVerified"])
        } else if (loginResponseBody.containsKey("accessToken")) {
            val accessToken = loginResponseBody["accessToken"] as String
            // Test accessing a protected endpoint with access token
            val protectedResponse = client.get("/api/v1/user/me") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }

            assertEquals(HttpStatusCode.OK, protectedResponse.status)
            val userInfo = Json.decodeFromString<Map<String, Any>>(protectedResponse.bodyAsText())
            assertEquals("test@example.com", userInfo["email"])
            assertEquals(true, userInfo["isVerified"])
        } else {
            throw AssertionError("Expected either 'token' or 'accessToken' in login response")
        }

        // 6. Try to login after logout (should fail)
        val loginAfterLogoutResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                    {
                        "email": "${testUser.email}",
                        "password": "${testUser.password}"
                    }
                    """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, loginAfterLogoutResponse.status)
        val loginAfterLogoutError = Json.decodeFromString<Map<String, String>>(loginAfterLogoutResponse.bodyAsText())
        assertEquals("Invalid email or password", loginAfterLogoutError["message"])
    }

    @Test
    fun `test invalid login attempts`() = runBlocking {
        com.repcheck.testutils.TestConfig.withTestApplication {
            val client = createTestClient()

            // 1. Try to login with non-existent user
            val nonExistentUserResponse = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "nonexistent@example.com",
                        "password": "password123"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.Unauthorized, nonExistentUserResponse.status)
            val error1 = Json.decodeFromString<Map<String, String>>(nonExistentUserResponse.bodyAsText())
            assertEquals("Invalid email or password", error1["message"])

            // 2. Create a test user first
            client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "${testUser.email}",
                        "password": "${testUser.password}"
                    }
                    """.trimIndent()
                )
            }

            // 3. Try to login with wrong password
            val wrongPasswordResponse = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "${testUser.email}",
                        "password": "wrongpassword"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.Unauthorized, wrongPasswordResponse.status)
            val error2 = Json.decodeFromString<Map<String, String>>(wrongPasswordResponse.bodyAsText())
            assertEquals("Invalid email or password", error2["message"])
        }
    }

    @Test
    fun `test password reset flow`() = runBlocking {
        com.repcheck.testutils.TestConfig.withTestApplication {
            val client = createTestClient()

            // 1. Register a test user
            client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "${testUser.email}",
                        "password": "${testUser.password}"
                    }
                    """.trimIndent()
                )
            }

            // 2. Request password reset
            val resetRequestResponse = client.post("/api/v1/auth/forgot-password") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(Parameters.build {
                    append("email", testUser.email)
                })
            }

            assertEquals(HttpStatusCode.OK, resetRequestResponse.status)
            val resetResponse = Json.decodeFromString<Map<String, String>>(resetRequestResponse.bodyAsText())
            assertEquals("Password reset email sent", resetResponse["message"])

            // 3. Reset password with token (mocking the token for testing)
            val resetToken = "test-reset-token"
            val newPassword = "newPassword123"

            val resetPasswordResponse = client.post("/api/v1/auth/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "token": "$resetToken",
                        "newPassword": "$newPassword"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.OK, resetPasswordResponse.status)
            val resetPasswordResponseBody =
                Json.decodeFromString<Map<String, String>>(resetPasswordResponse.bodyAsText())
            assertEquals("Password has been reset successfully", resetPasswordResponseBody["message"])

            // 4. Verify login with new password works
            val loginResponse = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "${testUser.email}",
                        "password": "$newPassword"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.OK, loginResponse.status)
            val loginResponseBody = Json.decodeFromString<Map<String, Any>>(loginResponse.bodyAsText())
            assertTrue(
                loginResponseBody.containsKey("token") || loginResponseBody.containsKey("accessToken"),
                "Expected either 'token' or 'accessToken' in login response"
            )
        }
    }

    @Test
    fun `test registration with existing email`() = runBlocking {
        com.repcheck.testutils.TestConfig.withTestApplication {
            val client = createTestClient()

            // 1. Register a test user
            val registerResponse1 = client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "${testUser.email}",
                        "password": "${testUser.password}"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.Created, registerResponse1.status)

            // 2. Try to register with the same email again
            val registerResponse2 = client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "${testUser.email}",
                        "password": "differentPassword123"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.Conflict, registerResponse2.status)
            val errorResponse = Json.decodeFromString<Map<String, String>>(registerResponse2.bodyAsText())
            assertTrue(errorResponse["message"]?.contains("already exists") == true)
        }
    }

    @Test
    fun `test logout flow`() = runBlocking {
        com.repcheck.testutils.TestConfig.withTestApplication {
            val client = createTestClient()

            // 1. Register a test user
            val registerResponse = client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "${testUser.email}",
                        "password": "${testUser.password}"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.Created, registerResponse.status)

            // 2. Login to get token
            val loginResponse = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "${testUser.email}",
                        "password": "${testUser.password}"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.OK, loginResponse.status)
            val loginResponseBody = loginResponse.body<Map<String, Any>>()

            // 3. Test logout
            if (loginResponseBody.containsKey("token") || loginResponseBody.containsKey("accessToken")) {
                val token = loginResponseBody["token"] as? String ?: loginResponseBody["accessToken"] as String

                val logoutResponse = client.post("/api/v1/auth/logout") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

                assertEquals(HttpStatusCode.OK, logoutResponse.status)

                // 4. Verify token is invalidated
                val protectedResponse = client.get("/api/v1/user/me") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    expectSuccess = false
                }

                assertEquals(HttpStatusCode.Unauthorized, protectedResponse.status)
            }
        }
    }

    @Test
    fun `test invalid registration attempts`() = runBlocking {
        com.repcheck.testutils.TestConfig.withTestApplication {
            val client = createTestClient()

            // 1. Invalid email
            val invalidRegisterResponse = client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "notanemail",
                        "password": "password123"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.BadRequest, invalidRegisterResponse.status)

            // 2. Short password
            val shortPasswordResponse = client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "test@example.com",
                        "password": "123"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.BadRequest, shortPasswordResponse.status)

            // 3. Missing fields
            val missingFieldsResponse = client.post("/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "test@example.com"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(HttpStatusCode.BadRequest, missingFieldsResponse.status)
        }
    }
}
