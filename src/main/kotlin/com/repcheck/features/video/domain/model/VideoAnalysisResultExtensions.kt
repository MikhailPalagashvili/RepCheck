package com.repcheck.features.video.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Extension function to convert VideoAnalysisResult to JSON string
 */
fun VideoAnalysisResult.toJson(): String {
    return Json.encodeToString(this)
}

/**
 * Extension function to parse JSON string to VideoAnalysisResult
 */
fun VideoAnalysisResult.Companion.fromJson(json: String): VideoAnalysisResult {
    return Json.decodeFromString<VideoAnalysisResult>(json)
}

// Add serialization to the VideoAnalysisResult class
@kotlinx.serialization.Serializable
class VideoAnalysisResult private constructor(
    val videoId: Long,
    val userId: Long,
    val workoutSetId: Long?,
    val score: Double,
    val feedback: String,
    val isValid: Boolean = true
) {
    companion object {
        /**
         * Creates a new VideoAnalysisResult
         */
        fun create(
            videoId: Long,
            userId: Long,
            workoutSetId: Long?,
            score: Double,
            feedback: String,
            isValid: Boolean = true
        ): VideoAnalysisResult {
            return VideoAnalysisResult(videoId, userId, workoutSetId, score, feedback, isValid)
        }

        /**
         * Creates an invalid analysis result with the given error message
         */
        fun invalid(videoId: Long, userId: Long, errorMessage: String, workoutSetId: Long? = null): VideoAnalysisResult {
            return create(
                videoId = videoId,
                userId = userId,
                workoutSetId = workoutSetId,
                score = 0.0,
                feedback = errorMessage,
                isValid = false
            )
        }

        /**
         * Parses a VideoAnalysisResult from JSON string
         */
        fun fromJson(json: String): VideoAnalysisResult {
            return Json.decodeFromString(json)
        }
    }

    /**
     * Converts the VideoAnalysisResult to JSON string
     */
    fun toJson(): String {
        return Json.encodeToString(this)
    }
}
