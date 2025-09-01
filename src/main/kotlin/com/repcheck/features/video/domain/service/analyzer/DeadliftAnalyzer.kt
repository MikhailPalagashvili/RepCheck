package com.repcheck.features.video.domain.service.analyzer

import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.service.VideoFeatures

/**
 * Analyzer for the Deadlift exercise following Starting Strength standards.
 */
object DeadliftAnalyzer : BaseExerciseAnalyzer() {
    override val exerciseType = ExerciseType.DEADLIFT

    // Thresholds for form feedback
    private val backAngleThreshold = 15.0 // Maximum acceptable back angle from vertical at start (degrees)
    private val barPathDeviationThreshold = 0.1f // Maximum allowed horizontal bar path deviation (normalized)
    private val lockoutHipExtensionThreshold = 170.0 // Minimum hip angle at lockout (degrees)

    override fun calculateMetrics(features: VideoFeatures): Map<String, Float> {
        val metrics = mutableMapOf<String, Float>()

        // TODO: Implement actual calculation from pose data
        // For now, return placeholder metrics
        metrics["back_angle_start"] = 20f
        metrics["bar_path_deviation"] = 0.08f
        metrics["lockout_hip_angle"] = 172f
        metrics["bar_speed"] = 1.2f
        metrics["hip_shoulder_rise_ratio"] = 0.95f

        return metrics
    }

    override fun generateFeedback(metrics: Map<String, Float>): List<String> {
        val feedback = mutableListOf<String>()

        // Check back angle at start
        metrics["back_angle_start"]?.let { angle ->
            if (angle > backAngleThreshold) {
                feedback.add("Set your back in extension before lifting - your back is too horizontal")
            }
        }

        // Check bar path
        metrics["bar_path_deviation"]?.let { deviation ->
            if (deviation > barPathDeviationThreshold) {
                feedback.add("Keep the bar close to your body - it's swinging out too far")
            }
        }

        // Check lockout
        metrics["lockout_hip_angle"]?.let { angle ->
            if (angle < lockoutHipExtensionThreshold) {
                feedback.add("Fully extend your hips at the top - squeeze your glutes")
            }
        }

        // Check hip-shoulder timing
        metrics["hip_shoulder_rise_ratio"]?.let { ratio ->
            if (ratio < 0.9f) {
                feedback.add("Hips and shoulders should rise together - don't let your hips shoot up first")
            }
        }

        // Add positive reinforcement if form is good
        if (feedback.isEmpty() && metrics.isNotEmpty()) {
            feedback.add("Good form! Keep the bar close and maintain a neutral spine.")
        }

        return feedback
    }

    override fun calculateFormScore(metrics: Map<String, Float>): Float {
        if (metrics.isEmpty()) return 0f

        var score = 0f
        var totalWeights = 0f

        // Back angle is critical (40% weight)
        metrics["back_angle_start"]?.let { angle ->
            val angleScore = 1.0f - (angle / backAngleThreshold.toFloat()).coerceIn(0f, 1f)
            score += angleScore * 0.4f
            totalWeights += 0.4f
        }

        // Bar path (30% weight)
        metrics["bar_path_deviation"]?.let { deviation ->
            val pathScore = 1.0f - (deviation / barPathDeviationThreshold).coerceIn(0f, 1f)
            score += pathScore * 0.3f
            totalWeights += 0.3f
        }

        // Lockout (20% weight)
        metrics["lockout_hip_angle"]?.let { angle ->
            val lockoutScore = (angle / lockoutHipExtensionThreshold.toFloat()).coerceIn(0f, 1f)
            score += lockoutScore * 0.2f
            totalWeights += 0.2f
        }

        // Other metrics (10% weight)
        val otherMetrics =
            metrics.filterKeys { it !in setOf("back_angle_start", "bar_path_deviation", "lockout_hip_angle") }
        if (otherMetrics.isNotEmpty()) {
            val otherScore = otherMetrics.values.average().toFloat()
            score += otherScore * 0.1f
            totalWeights += 0.1f
        }

        return if (totalWeights > 0) (score / totalWeights) else 0f
    }

    override fun countRepetitions(features: VideoFeatures): Int {
        // TODO: Implement repetition counting for deadlifts
        // This would analyze the vertical movement of the bar to count reps
        return 0
    }
}
