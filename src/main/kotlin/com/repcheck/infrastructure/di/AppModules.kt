package com.repcheck.infrastructure.di

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

    // S3 configuration will be added back when we reimplement it
}
