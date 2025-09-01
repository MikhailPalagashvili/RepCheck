package com.repcheck.features.video.domain.service.analyzer

import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.service.VideoFeatures
import kotlin.math.pow

/**
 * Interface for analyzing different types of exercises.
 * Each exercise type will have its own implementation that follows Starting Strength standards.
 */
interface ExerciseAnalyzer {
    val exerciseType: ExerciseType

    /**
     * Analyzes the video features and returns form feedback and metrics.
     *
     * @param features Extracted features from the video
     * @return Pair of feedback list and metrics map
     */
    fun analyzeForm(features: VideoFeatures): Pair<List<String>, Map<String, Float>>

    /**
     * Counts the number of valid repetitions in the video.
     */
    fun countRepetitions(features: VideoFeatures): Int

    /**
     * Calculates an overall form score from 0.0 to 1.0.
     */
    fun calculateFormScore(metrics: Map<String, Float>): Float
}

/**
 * Base class for exercise analyzers with common functionality.
 */
abstract class BaseExerciseAnalyzer : ExerciseAnalyzer {
    override fun analyzeForm(features: VideoFeatures): Pair<List<String>, Map<String, Float>> {
        val metrics = calculateMetrics(features)
        val feedback = generateFeedback(metrics)
        return feedback to metrics
    }

    /**
     * Calculate exercise-specific metrics from video features.
     */
    protected abstract fun calculateMetrics(features: VideoFeatures): Map<String, Float>

    /**
     * Generate feedback based on calculated metrics and Starting Strength standards.
     */
    protected abstract fun generateFeedback(metrics: Map<String, Float>): List<String>

    override fun countRepetitions(features: VideoFeatures): Int {
        // Default implementation - to be overridden by specific exercise analyzers
        // TODO: Implement basic repetition counting based on motion patterns
        return 0
    }

    override fun calculateFormScore(metrics: Map<String, Float>): Float {
        // Default implementation - to be overridden by specific exercise analyzers
        // Returns a score between 0.0 and 1.0 based on form metrics
        return 0f
    }

    /**
     * Helper method to calculate the angle between three points.
     */
    protected fun calculateAngle(
        a: Pair<Float, Float>,
        b: Pair<Float, Float>,
        c: Pair<Float, Float>
    ): Double {
        val ab = kotlin.math.sqrt(
            (b.first - a.first).toDouble().pow(2.0) +
                    (b.second - a.second).toDouble().pow(2.0)
        )
        val cb = kotlin.math.sqrt(
            (b.first - c.first).toDouble().pow(2.0) +
                    (b.second - c.second).toDouble().pow(2.0)
        )
        val ac = kotlin.math.sqrt(
            (c.first - a.first).toDouble().pow(2.0) +
                    (c.second - a.second).toDouble().pow(2.0)
        )

        return Math.acos((ab * ab + cb * cb - ac * ac) / (2 * ab * cb)) * (180.0 / Math.PI)
    }
}
