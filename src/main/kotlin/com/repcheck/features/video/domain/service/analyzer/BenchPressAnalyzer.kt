package com.repcheck.features.video.domain.service.analyzer

import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.service.VideoFeatures

/**
 * Analyzer for the Bench Press exercise following Starting Strength standards.
 */
object BenchPressAnalyzer : BaseExerciseAnalyzer() {
    override val exerciseType = ExerciseType.BENCH_PRESS
    // Starting Strength bench press standards
    private val barPathDeviationThreshold = 0.1f // Bar should move in a slight J-curve (normalized)
    private val elbowAngleAtBottom = 75.0 // Elbow angle at bottom (degrees) - slightly less than 90
    private val gripWidthTolerance = 0.1f // Acceptable grip width variation (normalized)
    private val archThreshold = 15.0 // Maximum acceptable arch in lower back (degrees)
    private val barSpeedThreshold = 0.6f // Minimum bar speed at sticking point (normalized)
    private val barTouchPoint = 0.5f // Where bar touches chest (0 = neck, 1 = sternum)

    override fun calculateMetrics(features: VideoFeatures): Map<String, Float> {
        val metrics = mutableMapOf<String, Float>()

        // Calculate metrics from pose data
        // These are placeholder implementations - actual implementation would use pose estimation
        
        // Basic metrics
        metrics["bar_path"] = 0.08f  // Deviation from ideal J-curve path
        metrics["elbow_angle"] = 78f  // Elbow angle at bottom
        metrics["bar_speed"] = 0.7f   // Bar speed (normalized)
        metrics["back_arch"] = 12f    // Back arch angle (degrees)
        metrics["wrist_position"] = 0.95f // 1.0 = neutral, <1.0 = bent back
        metrics["bar_touch"] = 0.5f   // Where bar touches chest (0 = neck, 1 = sternum)
        metrics["leg_drive"] = 0.6f   // Amount of leg drive (0-1)
        
        // Calculate Starting Strength specific metrics
        val properGrip = metrics["wrist_position"]!! >= 0.9f
        val goodTouchPoint = metrics["bar_touch"]!! in 0.4f..0.6f
        val properElbowAngle = metrics["elbow_angle"]!! in 70f..85f
        
        metrics["ss_approved"] = if (properGrip && goodTouchPoint && properElbowAngle) 1f else 0f
        
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
                feedback.add("Keep the bar path in a slight J-curve - it should move from your lower chest to over your shoulders")
            }
        }

        // Check elbow angle at bottom
        metrics["elbow_angle"]?.let { angle ->
            if (angle < elbowAngleAtBottom) {
                feedback.add("Keep your elbows at about 75 degrees at the bottom - don't let them flare out")
            }
        }

        // Check bar speed
        metrics["bar_speed"]?.let { speed ->
            if (speed < barSpeedThreshold) {
                feedback.add("Maintain consistent speed - don't pause at the bottom")
            }
        }

        // Check back arch
        metrics["back_arch"]?.let { arch ->
            if (arch > archThreshold) {
                feedback.add("Keep a slight arch in your lower back, but don't overarch")
            }
        }

        // Check wrist position
        metrics["wrist_position"]?.let { wrist ->
            if (wrist < 0.9f) {
                feedback.add("Keep your wrists straight - don't let them bend back")
            }
        }

        // Add positive reinforcement if form is good
        if (feedback.isEmpty() && metrics.isNotEmpty() && metrics["ss_approved"] == 1f) {
            feedback.add("Excellent form! Maintain tightness in your upper back and keep your shoulder blades retracted.")
        }

        return feedback
    }

    override fun calculateFormScore(metrics: Map<String, Float>): Float {
        if (metrics.isEmpty()) return 0f

        var score = 0f
        var totalWeights = 0f

        // Bar path is critical (40% weight)
        metrics["bar_path_deviation"]?.let { deviation ->
            val pathScore = 1.0f - (deviation / barPathDeviationThreshold).coerceIn(0f, 1f)
            score += pathScore * 0.4f
            totalWeights += 0.4f
        }

        // Elbow angle (25% weight)
        metrics["elbow_angle_bottom"]?.let { angle ->
            val angleScore = (angle / elbowAngleAtBottom.toFloat()).coerceIn(0f, 1f)
            score += angleScore * 0.25f
            totalWeights += 0.25f
        }

        // Bar speed (15% weight)
        metrics["bar_speed"]?.let { speed ->
            val speedScore = (speed / barSpeedThreshold).coerceIn(0f, 1f)
            score += speedScore * 0.15f
            totalWeights += 0.15f
        }

        // Other metrics (20% weight)
        val otherMetrics = metrics.filterKeys { it !in setOf("bar_path_deviation", "elbow_angle_bottom", "bar_speed") }
        if (otherMetrics.isNotEmpty()) {
            val otherScore = otherMetrics.values.average().toFloat()
            score += otherScore * 0.2f
            totalWeights += 0.2f
        }

        return if (totalWeights > 0) (score / totalWeights) else 0f
    }

    override fun countRepetitions(features: VideoFeatures): Int {
        // TODO: Implement repetition counting for bench press
        // This would analyze the vertical movement of the bar to count reps
        return 0
    }
}
