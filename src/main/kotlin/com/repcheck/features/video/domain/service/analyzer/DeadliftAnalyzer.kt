package com.repcheck.features.video.domain.service.analyzer

import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.service.VideoFeatures

/**
 * Analyzer for the Deadlift exercise following Starting Strength standards.
 */
object DeadliftAnalyzer : BaseExerciseAnalyzer() {
    override val exerciseType = ExerciseType.DEADLIFT

    // Starting Strength deadlift standards
    private val backAngleThreshold = 20.0 // Maximum back angle from vertical at start (degrees)
    private val barPathDeviationThreshold = 0.08f // Bar should move in a straight vertical line
    private val lockoutHipExtensionThreshold = 175.0 // Full hip extension at lockout (degrees)
    private val shoulderOverBarThreshold = 0.05f // Shoulder should be slightly in front of the bar
    private val barSpeedThreshold = 1.0f // Minimum bar speed (m/s)
    private val hipShoulderRiseRatio = 0.9f // Hips and shoulders should rise together

    override fun calculateMetrics(features: VideoFeatures): Map<String, Float> {
        val metrics = mutableMapOf<String, Float>()

        // Calculate metrics from pose data
        // These are placeholder implementations - actual implementation would use pose estimation
        
        // Basic metrics
        metrics["back_angle_start"] = 18f  // Back angle from vertical at start (degrees)
        metrics["bar_path"] = 0.06f       // Deviation from vertical bar path
        metrics["lockout_hip_angle"] = 174f // Hip angle at lockout (degrees)
        metrics["bar_speed"] = 1.1f       // Bar speed (m/s)
        metrics["shoulder_over_bar"] = 0.03f // Shoulder position relative to bar (positive = in front)
        metrics["hip_shoulder_rise"] = 0.88f // Hip to shoulder rise ratio
        metrics["bar_over_midfoot"] = 0.95f // Bar position over midfoot (1.0 = perfect)
        
        // Calculate Starting Strength specific metrics
        val properSetup = metrics["shoulder_over_bar"]!! > 0f && 
                         metrics["bar_over_midfoot"]!! >= 0.9f
        val goodLockout = metrics["lockout_hip_angle"]!! >= 170f
        val properBarPath = metrics["bar_path"]!! <= barPathDeviationThreshold
        
        metrics["ss_approved"] = if (properSetup && goodLockout && properBarPath) 1f else 0f
        
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
        metrics["back_angle_start"]?.let { angle ->
            if (angle > backAngleThreshold) {
                feedback.add("Set your back in extension before lifting - your back is too horizontal")
            }
        }

        // Check bar path
        metrics["bar_path"]?.let { deviation ->
            if (deviation > barPathDeviationThreshold) {
                feedback.add("Keep the bar in contact with your legs - it's drifting away from your body")
            }
        }

        // Check lockout
        metrics["lockout_hip_angle"]?.let { angle ->
            if (angle < lockoutHipExtensionThreshold) {
                feedback.add("Fully extend your hips at the top - squeeze your glutes")
            }
        }

        // Check shoulder position relative to bar
        metrics["shoulder_over_bar"]?.let { shoulderPos ->
            if (shoulderPos < 0) {
                feedback.add("Your shoulders should be slightly in front of the bar at the start")
            }
        }
        
        // Check bar position over midfoot
        metrics["bar_over_midfoot"]?.let { barPos ->
            if (barPos < 0.9f) {
                feedback.add("The bar should be over the middle of your foot at the start")
            }
        }

        // Add positive reinforcement if form is good
        if (feedback.isEmpty() && metrics.isNotEmpty() && metrics["ss_approved"] == 1f) {
            feedback.add("Excellent deadlift form! You're maintaining proper back position and bar path.")
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
