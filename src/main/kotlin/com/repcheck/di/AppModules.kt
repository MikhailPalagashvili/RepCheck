package com.repcheck.di

import com.repcheck.features.ai.domain.repository.AIFeedbackRepository
import com.repcheck.features.ai.infrastructure.db.repository.ExposedAIFeedbackRepository
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.features.video.infrastructure.db.repository.ExposedVideoRepository
import com.repcheck.features.workout.domain.repository.WorkoutRepository
import com.repcheck.features.workout.infrastructure.db.repository.ExposedWorkoutRepository
import org.koin.dsl.module

val appModule = module {
    // Repositories
    single<AIFeedbackRepository> { ExposedAIFeedbackRepository() }
    single<WorkoutRepository> { ExposedWorkoutRepository() }
    single<VideoRepository> { ExposedVideoRepository() }
}
