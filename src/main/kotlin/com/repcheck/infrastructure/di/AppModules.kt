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
import com.repcheck.features.video.domain.service.*
import com.repcheck.features.video.domain.service.metrics.VideoProcessingMetrics
import com.repcheck.features.video.infrastructure.repository.ExposedVideoRepository
import com.repcheck.features.workout.domain.repository.WorkoutRepository
import com.repcheck.features.workout.infrastructure.repository.ExposedWorkoutRepository
import com.repcheck.infrastructure.config.S3Config
import com.repcheck.infrastructure.s3.S3ClientProvider
import com.repcheck.infrastructure.s3.S3UploadService
import io.ktor.server.config.*
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

fun appModule(appConfig: ApplicationConfig) = module {

    // Make ApplicationConfig available to Koin
    single { appConfig }

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

    // S3UploadService singleton
    single {
        val s3Config = S3Config.fromConfig(get())
        S3ClientProvider.initialize(s3Config)
        S3UploadService(
            bucketName = s3Config.bucketName,
            presignedUrlExpiry = s3Config.presignedUrlExpiry
        )
    }


    single<VideoAnalysisService> {
        DefaultVideoAnalysisService()
    }
    single<ProcessingProgressTracker> {
        InMemoryProgressTracker()
    }
    single<VideoProcessor> {
        DefaultVideoProcessor(
            analysisService = get(),
            progressTracker = get()
        )
    }
    single {
        VideoService(
            videoRepository = get(),
            s3UploadService = get(),
            videoProcessor = get()
        )
    }

    // Metrics
    single {
        val registry = get<MeterRegistry>()
        VideoProcessingMetrics(registry)
    }

    // Video Processing
    single<VideoProcessingWorker> {
        VideoProcessingWorker(
            queueService = get(),
            videoRepository = get(),
            videoProcessor = get(),
            progressTracker = get()
        )
    }
}
