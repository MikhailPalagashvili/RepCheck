package com.repcheck.features.workout.domain.model

data class WorkoutSet(
    val id: Long = 0,
    val workoutId: Long,
    val liftId: Long,
    val weight: Double,
    val reps: Int,
    val rpe: Double? = null,
    val notes: String = "",
    val completedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
