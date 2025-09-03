package com.repcheck

import com.repcheck.features.user.application.service.AuthService
import com.repcheck.features.user.application.service.JwtProvider
import com.repcheck.features.user.infrastructure.web.authRoutes
import com.repcheck.features.video.domain.service.VideoService
import com.repcheck.features.video.presentation.videoRoutes
import com.repcheck.infrastructure.config.AppConfig
import com.repcheck.infrastructure.di.appModule
import com.repcheck.infrastructure.metrics.metricsModule
import com.repcheck.infrastructure.persistence.db.Database
import com.repcheck.infrastructure.web.plugins.installMonitoring
import com.repcheck.infrastructure.web.plugins.installSerialization
import com.repcheck.infrastructure.web.routes.healthRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.koin

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    // Initialize database
    val dbConfig = AppConfig.databaseConfig()
    Database.init(dbConfig)
    runFlywayMigrations()

    // Install Koin first to get dependencies
    koin {
        modules(appModule(environment.config), metricsModule)
    }

    // Get dependencies from Koin
    val jwtProvider = get<JwtProvider>()
    val authService = get<AuthService>()
    val videoService = get<VideoService>()

    // Configure application features
    configureFeatures()

    // Configure authentication before any routes
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtProvider.audience
            verifier(jwtProvider.verifier())
            validate { credential ->
                if (credential.payload.audience.contains(jwtProvider.audience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // Install Routing after authentication is configured
    install(Routing) {
        // Public health check endpoint
        healthRoutes()

        // API routes under /api/v1
        route("/api/v1") {
            // Public auth routes
            authRoutes(authService)

            // Protected routes
            authenticate("auth-jwt") {
                videoRoutes(videoService)
            }
        }
    }
}

private fun Application.configureFeatures() {
    install(CORS) {
        anyHost()
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowSameOrigin = true
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
    }

    install(DefaultHeaders) {
        header(HttpHeaders.Server, "RepCheck")
    }

    installMonitoring()
    installSerialization()
}

private fun runFlywayMigrations() {
    val dbConfig = AppConfig.databaseConfig()
    val flyway = Flyway.configure()
        .dataSource(
            dbConfig.url,
            dbConfig.user,
            dbConfig.password
        )
        .load()
    flyway.migrate()
}
