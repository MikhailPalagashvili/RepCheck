package com.repcheck.features.video.domain.service.analyzer

import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.service.VideoFeatures

/**
 * Analyzer for the Squat exercise following Starting Strength standards.
 */
object SquatAnalyzer : BaseExerciseAnalyzer() {
    override val exerciseType = ExerciseType.SQUAT

    // Ideal angles and ranges for key joints in degrees
    private val idealKneeAngleAtBottom = 80.0 // Approximate knee angle at bottom position
    private val idealHipAngleAtBottom = 30.0  // Approximate hip angle at bottom position
    private val idealAnkleAngleAtBottom = 30.0 // Approximate ankle angle at bottom position

    // Thresholds for form feedback
    private val kneeValgusThreshold = 15.0 // Degrees of knee valgus (knees caving in) to trigger feedback
    private val depthThreshold = 0.9f // Minimum depth as a fraction of full range (1.0 = parallel)
    private val barPathDeviationThreshold = 0.15f // Maximum allowed horizontal bar path deviation

    override fun calculateMetrics(features: VideoFeatures): Map<String, Float> {
        // If no keyframes, return empty metrics
        if (features.keyFrames.isEmpty()) {
            return emptyMap()
        }
        
        val metrics = mutableMapOf<String, Float>()

        // TODO: Implement actual calculation from pose data
        // For now, return placeholder metrics
        metrics["knee_angle"] = 85f
        metrics["hip_angle"] = 35f
        metrics["ankle_angle"] = 32f
        metrics["knee_valgus"] = 5f
        metrics["depth"] = 0.92f
        metrics["bar_path_deviation"] = 0.1f

        return metrics
    }

    override fun generateFeedback(metrics: Map<String, Float>): List<String> {
        val feedback = mutableListOf<String>()

        if (metrics.isEmpty()) {
            return emptyList()
        }

        // Check depth
        metrics["depth"]?.let { depth ->
            if (depth < depthThreshold) {
                feedback.add("Go deeper - aim to get your hip crease below the top of your knee")
            }
        }

        // Check knee valgus
        metrics["knee_valgus"]?.let { valgus ->
            if (valgus > kneeValgusThreshold) {
                feedback.add("Keep your knees out - they're caving in during the ascent")
            }
        }

        // Check bar path
        metrics["bar_path_deviation"]?.let { deviation ->
            if (deviation > 0.15f) {
                feedback.add("Keep the bar over mid-foot - it's moving forward/backward too much")
            }
        }

        // Add positive reinforcement if form is good
        if (feedback.isEmpty() && metrics.isNotEmpty()) {
            feedback.add("Good form! Keep your chest up and maintain tension throughout the movement.")
        }

        return feedback
    }

    override fun calculateFormScore(metrics: Map<String, Float>): Float {
        var score = 0f
        var totalWeights = 0f

        // Depth is the most important factor for squats
        metrics["depth"]?.let { depth ->
            val depthScore = (depth / depthThreshold).coerceAtMost(1f)
            score += depthScore * 0.4f
            totalWeights += 0.4f
        }

        // Knee valgus is a major form issue
        metrics["knee_valgus"]?.let { valgus ->
            val valgusScore = 1f - (valgus / kneeValgusThreshold.toFloat()).coerceIn(0f, 1f)
            score += valgusScore * 0.3f
            totalWeights += 0.3f
        }

        // Bar path deviation affects efficiency
        metrics["bar_path_deviation"]?.let { deviation ->
            val pathScore = 1f - (deviation / barPathDeviationThreshold).coerceIn(0f, 1f)
            score += pathScore * 0.2f
            totalWeights += 0.2f
        }

        // Other metrics contribute to the remaining 10%
        val otherMetrics = metrics.filterKeys { it !in setOf("depth", "knee_valgus", "bar_path_deviation") }
        if (otherMetrics.isNotEmpty()) {
            val otherScore = otherMetrics.values.average().toFloat()
            score += otherScore * 0.1f
            totalWeights += 0.1f
        }

        return if (totalWeights > 0) (score / totalWeights) else 0f
    }

    override fun countRepetitions(features: VideoFeatures): Int {
        // TODO: Implement repetition counting for squats
        // This would analyze the vertical movement of the hips to count reps
        return 0
    }
}