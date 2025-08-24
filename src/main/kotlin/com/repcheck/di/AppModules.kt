package com.repcheck.di

import com.repcheck.features.ai.domain.repository.AIFeedbackRepository
import com.repcheck.features.ai.infrastructure.db.repository.ExposedAIFeedbackRepository
import org.koin.dsl.module

val appModule = module {
    // Repositories
    single<AIFeedbackRepository> { ExposedAIFeedbackRepository() }
}
