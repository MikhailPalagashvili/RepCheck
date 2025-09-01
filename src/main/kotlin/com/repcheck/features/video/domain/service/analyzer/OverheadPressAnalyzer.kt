package com.repcheck.features.video.domain.service.analyzer

import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.service.VideoFeatures

/**
 * Analyzer for the Overhead Press exercise following Starting Strength standards.
 */
object OverheadPressAnalyzer : BaseExerciseAnalyzer() {
    override val exerciseType = ExerciseType.OVERHEAD_PRESS

    // Thresholds for form feedback
    private val barPathDeviationThreshold = 0.15f // Maximum allowed horizontal bar path deviation (normalized)
    private val hipThrustThreshold = 0.2f // Maximum acceptable hip thrust (normalized)
    private val lockoutElbowAngleThreshold = 170.0 // Minimum elbow angle at lockout (degrees)
    private val gripWidthTolerance = 0.1f // Acceptable grip width variation (normalized)

    override fun calculateMetrics(features: VideoFeatures): Map<String, Float> {
        val metrics = mutableMapOf<String, Float>()

        // TODO: Implement actual calculation from pose data
        // For now, return placeholder metrics
        metrics["bar_path_deviation"] = 0.12f
        metrics["hip_thrust"] = 0.15f
        metrics["lockout_elbow_angle"] = 172f
        metrics["grip_width_variation"] = 0.08f
        metrics["bar_speed"] = 0.7f

        return metrics
    }

    override fun generateFeedback(metrics: Map<String, Float>): List<String> {
        val feedback = mutableListOf<String>()

        // Check bar path
        metrics["bar_path_deviation"]?.let { deviation ->
            if (deviation > barPathDeviationThreshold) {
                feedback.add("Keep the bar close to your face - it should move in a straight vertical line")
            }
        }

        // Check hip thrust
        metrics["hip_thrust"]?.let { thrust ->
            if (thrust > hipThrustThreshold) {
                feedback.add("Minimize the hip thrust - use just enough to get past the sticking point")
            }
        }

        // Check lockout
        metrics["lockout_elbow_angle"]?.let { angle ->
            if (angle < lockoutElbowAngleThreshold) {
                feedback.add("Fully extend your elbows at the top - lock out completely")
            }
        }

        // Check grip width
        metrics["grip_width_variation"]?.let { variation ->
            if (variation > gripWidthTolerance) {
                feedback.add("Maintain consistent grip width - hands should be just outside shoulders")
            }
        }

        // Add positive reinforcement if form is good
        if (feedback.isEmpty() && metrics.isNotEmpty()) {
            feedback.add("Good form! Keep your core tight and press the bar in a straight line.")
        }

        return feedback
    }

    override fun calculateFormScore(metrics: Map<String, Float>): Float {
        if (metrics.isEmpty()) return 0f

        var score = 0f
        var totalWeights = 0f

        // Bar path is most important (40% weight)
        metrics["bar_path_deviation"]?.let { deviation ->
            val pathScore = 1.0f - (deviation / barPathDeviationThreshold).coerceIn(0f, 1f)
            score += pathScore * 0.4f
            totalWeights += 0.4f
        }

        // Hip thrust (25% weight)
        metrics["hip_thrust"]?.let { thrust ->
            val thrustScore = 1.0f - (thrust / hipThrustThreshold).coerceIn(0f, 1f)
            score += thrustScore * 0.25f
            totalWeights += 0.25f
        }

        // Lockout (20% weight)
        metrics["lockout_elbow_angle"]?.let { angle ->
            val lockoutScore = (angle / lockoutElbowAngleThreshold.toFloat()).coerceIn(0f, 1f)
            score += lockoutScore * 0.2f
            totalWeights += 0.2f
        }

        // Other metrics (15% weight)
        val otherMetrics =
            metrics.filterKeys { it !in setOf("bar_path_deviation", "hip_thrust", "lockout_elbow_angle") }
        if (otherMetrics.isNotEmpty()) {
            val otherScore = otherMetrics.values.average().toFloat()
            score += otherScore * 0.15f
            totalWeights += 0.15f
        }

        return if (totalWeights > 0) (score / totalWeights) else 0f
    }

    override fun countRepetitions(features: VideoFeatures): Int {
        // TODO: Implement repetition counting for overhead press
        // This would analyze the vertical movement of the bar to count reps
        return 0
    }
}
