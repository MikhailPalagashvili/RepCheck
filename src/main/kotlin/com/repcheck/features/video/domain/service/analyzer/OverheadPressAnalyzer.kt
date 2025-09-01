package com.repcheck.features.video.domain.service.analyzer

import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.service.VideoFeatures

/**
 * Analyzer for the Overhead Press exercise following Starting Strength standards.
 */
object OverheadPressAnalyzer : BaseExerciseAnalyzer() {
    override val exerciseType = ExerciseType.OVERHEAD_PRESS

    // Starting Strength Press standards
    private val barPathDeviationThreshold = 0.1f // Bar should move in a slight arc (normalized)
    private val hipThrustThreshold = 0.25f // Maximum acceptable hip thrust (normalized)
    private val lockoutElbowAngleThreshold = 175.0 // Full elbow extension at lockout (degrees)
    private val gripWidthTolerance = 0.08f // Grip should be just outside shoulders
    private val barSpeedThreshold = 0.6f // Minimum bar speed (normalized)
    private val headPositionThreshold = 0.15f // Head should move forward as bar passes

    override fun calculateMetrics(features: VideoFeatures): Map<String, Float> {
        val metrics = mutableMapOf<String, Float>()

        // Calculate metrics from pose data
        // These are placeholder implementations - actual implementation would use pose estimation
        
        // Basic metrics
        metrics["bar_path"] = 0.08f           // Deviation from ideal bar path
        metrics["hip_thrust"] = 0.18f         // Amount of hip thrust (0-1)
        metrics["lockout_elbow_angle"] = 174f // Elbow angle at lockout (degrees)
        metrics["grip_width"] = 0.07f         // Grip width variation from ideal
        metrics["bar_speed"] = 0.75f          // Bar speed (normalized)
        metrics["head_position"] = 0.12f       // Head movement (0-1)
        metrics["forearm_angle"] = 88f        // Forearm angle from vertical (degrees)
        
        // Calculate Starting Strength specific metrics
        val properGrip = metrics["grip_width"]!! <= gripWidthTolerance
        val goodLockout = metrics["lockout_elbow_angle"]!! >= lockoutElbowAngleThreshold
        val properForearms = metrics["forearm_angle"]!! in 85f..90f
        
        metrics["ss_approved"] = if (properGrip && goodLockout && properForearms) 1f else 0f
        
        return metrics
    }

    override fun generateFeedback(metrics: Map<String, Float>): List<String> {
        val feedback = mutableListOf<String>()

        if (metrics.isEmpty()) {
            return emptyList()
        }
        
        // Starting Strength specific feedback
        metrics["ss_approved"]?.let { 
            if (it < 1f) {
                feedback.add("Form needs adjustment to match Starting Strength standards")
            }
        }
        metrics["bar_path"]?.let { deviation ->
            if (deviation > barPathDeviationThreshold) {
                feedback.add("Keep the bar close to your face - it should move in a slight arc from your shoulders to overhead")
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
        metrics["grip_width"]?.let { gripWidth ->
            if (gripWidth > gripWidthTolerance) {
                feedback.add("Grip should be just outside shoulders - your grip might be too wide")
            }
        }

        // Check head position
        metrics["head_position"]?.let { headPos ->
            if (headPos < headPositionThreshold) {
                feedback.add("Move your head forward as the bar passes - look forward, not up")
            }
        }

        // Add positive reinforcement if form is good
        if (feedback.isEmpty() && metrics.isNotEmpty() && metrics["ss_approved"] == 1f) {
            feedback.add("Excellent press form! Keep your forearms vertical and press the bar in a smooth motion.")
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
