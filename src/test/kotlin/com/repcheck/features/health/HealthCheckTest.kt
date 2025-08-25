package com.repcheck.features.health

import com.repcheck.testutils.BaseTest
import com.repcheck.testutils.TestConfig
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class HealthCheckTest : BaseTest() {
    @Test
    fun `test health check endpoint`() = runBlocking {
        TestConfig.withTestApplication {
            val client = createTestClient()
            val response = client.get("/health")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}
