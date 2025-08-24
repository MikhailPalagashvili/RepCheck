package com.repcheck.features.workout.domain.repository

import com.repcheck.features.workout.domain.model.Workout
import com.repcheck.features.workout.domain.model.WorkoutSet

interface WorkoutRepository {
    fun createWorkout(
        userId: Long,
        startTimeMillis: Long = System.currentTimeMillis(),
        notes: String? = null,
    ): Workout

    fun addSet(
        workoutId: Long,
        liftId: Long,
        weight: Double,
        reps: Int,
        rpe: Double? = null,
        notes: String? = null,
        completedAtMillis: Long = System.currentTimeMillis(),
    ): WorkoutSet

    fun listWorkoutsByUser(
        userId: Long,
        limit: Int = 50,
        offset: Long = 0,
    ): List<Workout>

    fun listSetsByWorkout(workoutId: Long): List<WorkoutSet>

    fun deleteWorkout(workoutId: Long): Boolean
}
