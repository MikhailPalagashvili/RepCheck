package com.repcheck.di

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.repcheck.config.AppConfig
import com.repcheck.features.ai.domain.repository.AIFeedbackRepository
import com.repcheck.features.ai.infrastructure.repository.ExposedAIFeedbackRepository
import com.repcheck.features.user.application.service.AuthService
import com.repcheck.features.user.application.service.ConsoleEmailService
import com.repcheck.features.user.application.service.EmailService
import com.repcheck.features.user.application.service.JwtProvider
import com.repcheck.features.user.domain.repository.UserRepository
import com.repcheck.features.user.infrastructure.repository.ExposedUserRepository
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.features.video.infrastructure.repository.ExposedVideoRepository
import com.repcheck.features.workout.domain.repository.WorkoutRepository
import com.repcheck.features.workout.infrastructure.repository.ExposedWorkoutRepository
import com.repcheck.infrastructure.storage.S3Config
import com.repcheck.infrastructure.storage.S3ClientFactory
import com.repcheck.infrastructure.storage.S3Service
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

data class AuthConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String = "RepCheck API"
)

val appModule = module {
    // Repositories
    single<AIFeedbackRepository> { ExposedAIFeedbackRepository() }
    single<WorkoutRepository> { ExposedWorkoutRepository() }
    single<VideoRepository> { ExposedVideoRepository() }
    single<UserRepository> { ExposedUserRepository() }

    // Auth Services
    single { JwtProvider() }
    single<EmailService> { ConsoleEmailService() }
    single {
        AuthService(
            userRepository = get(),
            jwt = get(),
            emailService = get(),
            coroutineScope = CoroutineScope(Dispatchers.IO)
        )
    }

    // Storage Services
    single { S3Config.createFromEnv() }
    single { S3ClientFactory.createS3Client(get()) }
    single { S3ClientFactory.createS3Presigner(get()) }
    single {
        S3Service(
            s3Client = get(),
            s3Presigner = get(),
            config = get()
        )
    }

    // Auth Configuration
    single {
        val jwtConfig = AppConfig.jwtConfig()
        AuthConfig(
            secret = jwtConfig.secret,
            issuer = jwtConfig.issuer,
            audience = jwtConfig.audience
        )
    }
}

// Install and configure JWT authentication
fun Application.configureAuth(authConfig: AuthConfig) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = authConfig.realm
            verifier {
                JWT
                    .require(Algorithm.HMAC256(authConfig.secret))
                    .withAudience(authConfig.audience)
                    .withIssuer(authConfig.issuer)
                    .build()
            }
            validate { credential ->
                if (credential.payload.audience.contains(authConfig.audience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}