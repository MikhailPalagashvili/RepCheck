package com.repcheck.features.user.application

import com.repcheck.testutils.BaseTest
import com.repcheck.testutils.TestConfig
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SimpleTest : BaseTest() {
    @Test
    fun `test hello endpoint`() = runBlocking {
        TestConfig.withTestApplication {
            val client = createTestClient()
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}
