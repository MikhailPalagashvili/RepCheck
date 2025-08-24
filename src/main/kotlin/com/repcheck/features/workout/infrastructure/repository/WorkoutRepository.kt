package com.repcheck.features.workout.infrastructure.db.repository

import com.repcheck.features.workout.domain.model.Workout
import com.repcheck.features.workout.domain.model.WorkoutSet
import com.repcheck.features.workout.domain.repository.WorkoutRepository
import com.repcheck.features.workout.infrastructure.db.tables.WorkoutSets
import com.repcheck.features.workout.infrastructure.db.tables.Workouts
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ExposedWorkoutRepository : WorkoutRepository {
    private fun rowToWorkout(row: ResultRow): Workout = Workout(
        id = row[Workouts.id],
        userId = row[Workouts.userId],
        startTime = row[Workouts.startTime].toEpochMilli(),
        endTime = row[Workouts.endTime]?.toEpochMilli(),
        notes = row[Workouts.notes] ?: "",
        createdAt = row[Workouts.createdAt].toEpochMilli(),
        updatedAt = row[Workouts.updatedAt].toEpochMilli(),
    )

    private fun rowToWorkoutSet(row: ResultRow): WorkoutSet = WorkoutSet(
        id = row[WorkoutSets.id],
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
        val id = Workouts.insert { st ->
            st[Workouts.userId] = userId
            st[Workouts.startTime] = Instant.ofEpochMilli(startTimeMillis)
            st[Workouts.notes] = notes
        }[Workouts.id]

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
        val id = WorkoutSets.insert { st ->
            st[WorkoutSets.workoutId] = workoutId
            st[WorkoutSets.liftId] = liftId
            st[WorkoutSets.weight] = weight
            st[WorkoutSets.reps] = reps
            st[WorkoutSets.rpe] = rpe
            st[WorkoutSets.notes] = notes
            st[WorkoutSets.completedAt] = Instant.ofEpochMilli(completedAtMillis)
        }[WorkoutSets.id]

        WorkoutSets.select { WorkoutSets.id eq id }
            .single()
            .let(::rowToWorkoutSet)
    }

    override fun listWorkoutsByUser(userId: Long, limit: Int, offset: Long): List<Workout> = transaction {
        Workouts.select { Workouts.userId eq userId }
            .orderBy(Workouts.createdAt to SortOrder.DESC)
            .limit(limit, offset)
            .map(::rowToWorkout)
    }

    override fun listSetsByWorkout(workoutId: Long): List<WorkoutSet> = transaction {
        WorkoutSets.select { WorkoutSets.workoutId eq workoutId }
            .orderBy(WorkoutSets.completedAt to SortOrder.ASC)
            .map(::rowToWorkoutSet)
    }

    override fun deleteWorkout(workoutId: Long): Boolean = transaction {
        Workouts.deleteWhere { Workouts.id eq workoutId } > 0
    }
}
