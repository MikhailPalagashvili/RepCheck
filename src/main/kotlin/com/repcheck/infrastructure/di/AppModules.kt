package com.repcheck.infrastructure.di

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import com.repcheck.infrastructure.config.S3Config
import com.repcheck.infrastructure.s3.S3ClientProvider
import com.repcheck.infrastructure.storage.S3Service
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

data class AuthConfig(
    val jwtAudience: String,
    val jwtRealm: String,
    val jwtIssuer: String,
    val jwtSecret: String
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

// S3 Configuration - created once
    single { S3Config.fromConfig(get<ApplicationConfig>()) }

// S3 Client Provider - depends on config
    single {
        val config = get<S3Config>()
        S3ClientProvider.apply {
            initialize(config)
        }
    }

// S3 Client - from the provider
    single { get<S3ClientProvider>().s3Client }

// S3 Presigner - from the provider
    single { get<S3ClientProvider>().s3Presigner }

// S3 Service - depends on client, presigner, and config
    single {
        S3Service(
            s3Client = get(),
            s3Presigner = get(),
            config = get()  // This gets the S3Config we created first
        )
    }
}

// Install and configure JWT authentication
fun Application.configureAuth(authConfig: AuthConfig) {
    install(Authentication) {
        jwt {
            realm = authConfig.jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(authConfig.jwtSecret))
                    .withAudience(authConfig.jwtAudience)
                    .withIssuer(authConfig.jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(authConfig.jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
