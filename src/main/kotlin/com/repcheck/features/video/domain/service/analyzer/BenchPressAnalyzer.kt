package com.repcheck.features.video.domain.service.analyzer

import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.service.VideoFeatures

/**
 * Analyzer for the Bench Press exercise following Starting Strength standards.
 */
object BenchPressAnalyzer : BaseExerciseAnalyzer() {
    override val exerciseType = ExerciseType.BENCH_PRESS
    // Thresholds for form feedback
    private val barPathDeviationThreshold = 0.15f // Maximum allowed horizontal bar path deviation (normalized)
    private val elbowAngleAtBottomThreshold = 75.0 // Minimum elbow angle at bottom position (degrees)
    private val barSpeedThreshold = 0.5f // Minimum bar speed at sticking point (normalized)
    private val archThreshold = 10.0 // Maximum acceptable arch in lower back (degrees)

    override fun calculateMetrics(features: VideoFeatures): Map<String, Float> {
        val metrics = mutableMapOf<String, Float>()

        // TODO: Implement actual calculation from pose data
        // For now, return placeholder metrics
        metrics["bar_path_deviation"] = 0.12f
        metrics["elbow_angle_bottom"] = 80f
        metrics["bar_speed"] = 0.6f
        metrics["back_arch"] = 8f
        metrics["wrist_position"] = 0.9f // 1.0 = neutral, <1.0 = bent back

        return metrics
    }

    override fun generateFeedback(metrics: Map<String, Float>): List<String> {
        val feedback = mutableListOf<String>()

        // Check bar path
        metrics["bar_path_deviation"]?.let { deviation ->
            if (deviation > barPathDeviationThreshold) {
                feedback.add("Keep the bar path straight - it should move directly up and down over your shoulders")
            }
        }

        // Check elbow angle at bottom
        metrics["elbow_angle_bottom"]?.let { angle ->
            if (angle < elbowAngleAtBottomThreshold) {
                feedback.add("Don't let your elbows drop too low - keep them at about 75 degrees at the bottom")
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
        if (feedback.isEmpty() && metrics.isNotEmpty()) {
            feedback.add("Good form! Keep your shoulder blades retracted and drive through your legs.")
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
            val angleScore = (angle / elbowAngleAtBottomThreshold.toFloat()).coerceIn(0f, 1f)
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
