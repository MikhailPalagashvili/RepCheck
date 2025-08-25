package com.repcheck

import com.repcheck.config.AppConfig
import com.repcheck.db.Database
import com.repcheck.di.AuthConfig
import com.repcheck.di.appModule
import com.repcheck.di.configureAuth
import com.repcheck.features.user.application.service.AuthService
import com.repcheck.features.user.infrastructure.web.authRoutes
import com.repcheck.infrastructure.web.plugins.installMonitoring
import com.repcheck.infrastructure.web.plugins.installSerialization
import com.repcheck.infrastructure.web.routes.healthRoutes
import io.ktor.http.*
import io.ktor.server.application.*
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

    // Run migrations
    runFlywayMigrations()

    // Install Koin
    koin {
        modules(appModule)
    }

    // Get dependencies from Koin
    val authConfig = get<AuthConfig>()
    val authService = get<AuthService>()

    // Configure application features
    configureFeatures()

    // Configure authentication
    configureAuth(authConfig)

    // Setup routing
    install(Routing) {
        // Public health check endpoint - no rate limiting
        healthRoutes()

        // Auth routes with rate limiting
        route("/api/v1/auth") {
            // Install auth routes
            authRoutes(authService)
        }
    }
}

private fun Application.configureFeatures() {
    // Install CORS
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

    // Install default headers
    install(DefaultHeaders) {
        header(HttpHeaders.Server, "RepCheck")
    }

    // Install monitoring
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
