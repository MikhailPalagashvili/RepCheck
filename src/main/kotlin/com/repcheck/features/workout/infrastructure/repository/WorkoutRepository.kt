package com.repcheck.features.workout.infrastructure.repository

import com.repcheck.features.workout.domain.model.Workout
import com.repcheck.features.workout.domain.model.WorkoutSet
import com.repcheck.features.workout.domain.repository.WorkoutRepository
import com.repcheck.features.workout.infrastructure.table.WorkoutSets
import com.repcheck.features.workout.infrastructure.table.Workouts
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ExposedWorkoutRepository : WorkoutRepository {
    private fun rowToWorkout(row: ResultRow): Workout = Workout(
        id = row[Workouts.id].value,
        userId = row[Workouts.userId],
        startTime = row[Workouts.startTime].toEpochMilli(),
        endTime = row[Workouts.endTime]?.toEpochMilli(),
        notes = row[Workouts.notes] ?: "",
        createdAt = row[Workouts.createdAt].toEpochMilli(),
        updatedAt = row[Workouts.updatedAt].toEpochMilli(),
    )

    private fun rowToWorkoutSet(row: ResultRow): WorkoutSet = WorkoutSet(
        id = row[WorkoutSets.id].value,
        workoutId = row[WorkoutSets.workoutId],
        liftId = row[WorkoutSets.liftId],
        weight = row[WorkoutSets.weight],
        reps = row[WorkoutSets.reps],
        rpe = row[WorkoutSets.rpe],
        notes = row[WorkoutSets.notes] ?: "",
        completedAt = row[WorkoutSets.completedAt].toEpochMilli(),
        createdAt = row[WorkoutSets.createdAt].toEpochMilli(),
        updatedAt = row[WorkoutSets.updatedAt].toEpochMilli(),
    )

    override fun createWorkout(
        userId: Long,
        startTimeMillis: Long,
        notes: String?,
    ): Workout = transaction {
        val id = Workouts.insertAndGetId { workout ->
            workout[Workouts.userId] = userId
            workout[Workouts.startTime] = Instant.ofEpochMilli(startTimeMillis)
            workout[Workouts.notes] = notes
        }

        Workouts.select { Workouts.id eq id }
            .single()
            .let(::rowToWorkout)
    }

    override fun addSet(
        workoutId: Long,
        liftId: Long,
        weight: Double,
        reps: Int,
        rpe: Double?,
        notes: String?,
        completedAtMillis: Long,
    ): WorkoutSet = transaction {
        val id = WorkoutSets.insertAndGetId { set ->
            set[WorkoutSets.workoutId] = workoutId
            set[WorkoutSets.liftId] = liftId
            set[WorkoutSets.weight] = weight
            set[WorkoutSets.reps] = reps
            set[WorkoutSets.rpe] = rpe
            set[WorkoutSets.notes] = notes
            set[WorkoutSets.completedAt] = Instant.ofEpochMilli(completedAtMillis)
        }

        WorkoutSets.select { WorkoutSets.id eq id }
            .map(::rowToWorkoutSet)
            .single()
    }

    override fun listWorkoutsByUser(userId: Long, limit: Int, offset: Long): List<Workout> = transaction {
        Workouts.select { Workouts.userId eq userId }
            .orderBy(Workouts.startTime to SortOrder.DESC)
            .limit(n = limit, offset = offset)
            .map(::rowToWorkout)
    }

    override fun listSetsByWorkout(workoutId: Long): List<WorkoutSet> = transaction {
        WorkoutSets.select { WorkoutSets.workoutId eq workoutId }
            .orderBy(WorkoutSets.completedAt to SortOrder.ASC)
            .map(::rowToWorkoutSet)
    }

    override fun deleteWorkout(workoutId: Long): Boolean = transaction {
        Workouts.deleteWhere { Workouts.id eq EntityID(workoutId, Workouts) } > 0
    }
}
