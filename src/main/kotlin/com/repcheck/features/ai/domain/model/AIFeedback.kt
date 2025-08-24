package com.repcheck.features.ai.domain.model

data class AIFeedback(
    val id: Long = 0,
    val videoId: Long,
    val workoutSetId: Long,
    val analysisResults: AnalysisResults,
    val status: AnalysisStatus = AnalysisStatus.PENDING,
    val error: String? = null,
    val processedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AnalysisResults(
    val depthScore: Float? = null,
    val barPathScore: Float? = null,
    val symmetryScore: Float? = null,
    val overallScore: Float? = null,
    val feedback: List<String> = emptyList(),
    val rawData: Map<String, Any> = emptyMap()
)

enum class AnalysisStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
