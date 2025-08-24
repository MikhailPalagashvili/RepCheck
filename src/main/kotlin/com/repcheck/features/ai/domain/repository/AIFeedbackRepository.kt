package com.repcheck.features.ai.domain.repository

import com.repcheck.features.ai.domain.model.AIFeedback
import com.repcheck.features.ai.domain.model.AnalysisResults
import com.repcheck.features.ai.domain.model.AnalysisStatus
import java.time.Instant

interface AIFeedbackRepository {
    fun insert(
        videoId: Long,
        workoutSetId: Long,
        analysisResults: AnalysisResults,
        status: AnalysisStatus = AnalysisStatus.PENDING,
        error: String? = null,
        processedAt: Instant? = null,
    ): AIFeedback

    fun updateStatus(
        id: Long,
        status: AnalysisStatus,
        error: String? = null,
        processedAt: Instant? = null,
    ): Boolean

    fun findById(id: Long): AIFeedback?

    fun findByVideoId(videoId: Long): List<AIFeedback>

    fun findByWorkoutSetId(workoutSetId: Long): List<AIFeedback>

    fun delete(id: Long): Boolean
}
