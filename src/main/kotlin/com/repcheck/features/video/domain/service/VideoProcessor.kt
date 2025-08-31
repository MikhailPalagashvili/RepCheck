package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoAnalysis
import com.repcheck.features.video.domain.model.VideoProcessingProgress
import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import org.slf4j.LoggerFactory
import java.time.Instant

interface VideoProcessor {
    suspend fun process(video: WorkoutVideo): ProcessingResult
    suspend fun getProcessingProgress(videoId: Long): Int?
}

data class ProcessingResult(
    val videoId: Long,
    val status: VideoStatus,
    val analysis: VideoAnalysis? = null,
    val error: String? = null,
    val processedAt: Instant = Instant.now()
)

class DefaultVideoProcessor(
    private val analysisService: VideoAnalysisService,
    private val progressTracker: ProcessingProgressTracker
) : VideoProcessor {
    private val logger = LoggerFactory.getLogger(DefaultVideoProcessor::class.java)

    override suspend fun process(video: WorkoutVideo): ProcessingResult {
        return try {
            progressTracker.updateProgress(video.id, VideoProcessingProgress.INITIAL)
            validateVideo(video)

            progressTracker.updateProgress(video.id, VideoProcessingProgress.VALIDATION_COMPLETE)

            progressTracker.updateProgress(video.id, VideoProcessingProgress.FEATURE_EXTRACTION_START)
            val features = analysisService.extractFeatures(video)
            progressTracker.updateProgress(video.id, VideoProcessingProgress.FEATURE_EXTRACTION_COMPLETE)

            progressTracker.updateProgress(video.id, VideoProcessingProgress.ANALYSIS_START)
            val analysis = analysisService.analyzeWorkout(features)
            progressTracker.updateProgress(video.id, VideoProcessingProgress.ANALYSIS_COMPLETE)

            progressTracker.updateProgress(video.id, VideoProcessingProgress.REPORT_GENERATION_START)
            val report = analysisService.generateReport(analysis)
            progressTracker.updateProgress(video.id, VideoProcessingProgress.REPORT_GENERATION_COMPLETE)

            progressTracker.updateProgress(video.id, VideoProcessingProgress.COMPLETE)

            ProcessingResult(
                videoId = video.id,
                status = VideoStatus.PROCESSED,
                analysis = report
            )
        } catch (e: Exception) {
            logger.error("Error processing video: ${video.id}", e)
            ProcessingResult(
                videoId = video.id,
                status = VideoStatus.FAILED,
                error = e.message ?: "Unknown error"
            )
        }
    }

    override suspend fun getProcessingProgress(videoId: Long): Int? {
        return progressTracker.getProgress(videoId)
    }

    private fun validateVideo(video: WorkoutVideo) {
        // Basic validation
        require(video.s3Key.isNotBlank()) { "S3 key cannot be empty" }
        require(video.s3Bucket.isNotBlank()) { "S3 bucket cannot be empty" }
        // Add more validations as needed
    }
}