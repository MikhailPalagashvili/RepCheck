package com.repcheck.testutils

import com.repcheck.di.appModule
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Provides a preconfigured test application environment.
 */
object TestConfig {
    private const val TEST_JWT_SECRET = "test-secret"
    private const val TEST_JWT_ISSUER = "test-issuer"
    private const val TEST_JWT_AUDIENCE = "test-audience"
    private const val TEST_JWT_REALM = "test-realm"

    private fun Application.configureTestApplication() {
        // Start Koin (and stop on shutdown)
        startKoin { modules(appModule) }
        environment.monitor.subscribe(ApplicationStopped) { stopKoin() }

        // Override config values for JWT
        val config = environment.config as MapApplicationConfig
        config.apply {
            put("jwt.secret", TEST_JWT_SECRET)
            put("jwt.issuer", TEST_JWT_ISSUER)
            put("jwt.audience", TEST_JWT_AUDIENCE)
            put("jwt.realm", TEST_JWT_REALM)
        }

        // Install common plugins
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
        install(DefaultHeaders)
        install(CallLogging)
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Internal Server Error: ${cause.message}"
                )
            }
        }
    }

    suspend fun withConfiguredTestApplication(
        configure: Application.() -> Unit = {},
        test: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        environment {
            // Force MapApplicationConfig so configureTestApplication() won’t crash
            config = MapApplicationConfig()
        }
        application {
            configureTestApplication()
            configure()
        }
        test(this)
    }
}
