package com.repcheck.features.health

import com.repcheck.infrastructure.web.routes.healthRoutes
import com.repcheck.testutils.BaseTest
import com.repcheck.testutils.TestConfig.withConfiguredTestApplication
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthCheckTest : BaseTest() {
    @Test
    fun `test health check endpoint`() = runBlocking {
        withConfiguredTestApplication {
            application {
                routing {
                    healthRoutes()
                }
            }
            val response = client.get("/health/live")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `test health ready endpoint`() = runBlocking {
        withConfiguredTestApplication {
            application {
                routing {
                    healthRoutes()
                }
            }
            val response = client.get("/health/ready")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}
