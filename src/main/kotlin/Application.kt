package com.repcheck

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.repcheck.features.user.application.service.AuthService
import com.repcheck.features.user.infrastructure.web.authRoutes
import com.repcheck.features.video.presentation.videoRoutes
import com.repcheck.infrastructure.config.AppConfig
import com.repcheck.infrastructure.config.S3Config
import com.repcheck.infrastructure.di.AuthConfig
import com.repcheck.infrastructure.di.appModule
import com.repcheck.infrastructure.persistence.db.Database
import com.repcheck.infrastructure.s3.S3ClientProvider
import com.repcheck.infrastructure.s3.S3UploadService
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
import org.koin.dsl.module
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

    // Get JWT secret from environment or use a default for development
    val jwtSecret = environment.config.propertyOrNull("jwt.secret")?.getString()
        ?: "default-dev-secret-replace-in-production"

    // Install Koin with configuration
    koin {
        modules(
            module {
                single {
                    AuthConfig(
                        jwtAudience = "repcheck-app",
                        jwtRealm = "repcheck-realm",
                        jwtIssuer = "repcheck",
                        jwtSecret = jwtSecret
                    )
                }
            },
            appModule
        )
    }

    // Get dependencies from Koin
    val authConfig = get<AuthConfig>()
    val authService = get<AuthService>()

    // Initialize S3 configuration
    val s3Config = S3Config.fromConfig(environment.config)
    S3ClientProvider.initialize(s3Config)

    val s3UploadService = S3UploadService(
        bucketName = s3Config.bucketName,
        presignedUrlExpiry = s3Config.presignedUrlExpiry
    )

    // Configure application features
    configureFeatures()

    // Configure authentication - This must be called before any routes that use it
    install(Authentication) {
        jwt("auth-jwt") {
            realm = authConfig.jwtRealm
            verifier {
                JWT
                    .require(Algorithm.HMAC256(authConfig.jwtSecret))
                    .withAudience(authConfig.jwtAudience)
                    .withIssuer(authConfig.jwtIssuer)
                    .build()
            }
            validate { credential ->
                if (credential.payload.audience.contains(authConfig.jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // Setup routing with authentication
    install(Routing) {
        // Public health check endpoint - no authentication needed
        healthRoutes()

        // Auth routes
        route("/api/v1/auth") {
            authRoutes(authService)
        }

        // Protected routes
        authenticate("auth-jwt") {
            videoRoutes(s3UploadService)
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
