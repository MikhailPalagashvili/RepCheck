package com.repcheck

import com.repcheck.config.AppConfig
import com.repcheck.db.Database
import com.repcheck.di.appModule
import com.repcheck.features.auth.AuthService
import com.repcheck.features.auth.JwtProvider
import com.repcheck.features.auth.authRoutes
import com.repcheck.features.ratelimit.RateLimit
import com.repcheck.features.ratelimit.RateLimitConfig
import com.repcheck.web.plugins.installMonitoring
import com.repcheck.web.plugins.installSerialization
import com.repcheck.web.routes.healthRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway.configure
import org.koin.ktor.plugin.Koin

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

    // Dependency Injection
    install(Koin) {
        modules(appModule)
    }

    // Initialize services
    val jwt = JwtProvider()
    val authService = AuthService(jwt)

    installMonitoring()
    installSerialization()

    // Configure Rate Limit for auth endpoints
    val authRateLimit = RateLimit(
        RateLimitConfig(
            requestsPerMinute = 100,  // 100 requests per minute
            message = "Too many requests, please try again later."
        )
    )

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(jwt.verifier())
            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
        }
    }
    routing {
        // Public health check endpoint - no rate limiting
        healthRoutes()

        // Install auth routes with rate limiting
        authRoutes(authService, jwt)

        // Apply rate limiting to all auth routes
        route("/api/v1/auth") {
            intercept(ApplicationCallPipeline.Call) {
                authRateLimit.intercept(this) {
                    proceed()
                }
            }
        }
    }
}

private fun Application.runFlywayMigrations() {
    val dbConfig = AppConfig.databaseConfig()
    val flyway = configure().dataSource(
        dbConfig.url,
        dbConfig.user,
        dbConfig.password
    ).load()
    flyway.migrate()
}
