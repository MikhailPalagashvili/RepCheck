package com.repcheck.testutils

import com.repcheck.features.video.infrastructure.table.WorkoutVideos
import com.repcheck.infrastructure.di.appModule
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
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

object TestConfig {
    private const val TEST_JWT_SECRET = "test-secret"
    private const val TEST_JWT_ISSUER = "test-issuer"
    private const val TEST_JWT_AUDIENCE = "test-audience"
    private const val TEST_JWT_REALM = "test-realm"

    private fun Application.configureTestApplication(appConfig: ApplicationConfig = MapApplicationConfig()) {
        stopKoin()
        startKoin { modules(appModule(appConfig)) }
        environment.monitor.subscribe(ApplicationStopped) { stopKoin() }

        // Connect Exposed to an in-memory H2 database
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )

        // Create all necessary tables for testing
        transaction {
            SchemaUtils.create(WorkoutVideos)
        }

        // Install Ktor plugins
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
                    io.ktor.http.HttpStatusCode.InternalServerError,
                    "Internal Server Error: ${cause.message}"
                )
            }
        }
    }

    suspend fun withConfiguredTestApplication(
        configure: Application.() -> Unit = {},
        test: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        val mapConfig = MapApplicationConfig(
            "jwt.secret" to TEST_JWT_SECRET,
            "jwt.issuer" to TEST_JWT_ISSUER,
            "jwt.audience" to TEST_JWT_AUDIENCE,
            "jwt.realm" to TEST_JWT_REALM
        )

        environment {
            config = mapConfig
        }

        application {
            configureTestApplication(mapConfig)
            configure()
        }

        test(this)
    }
}
