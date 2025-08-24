package com.repcheck.features.workout.infrastructure.table

import com.repcheck.features.workout.domain.model.LiftType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Lifts : Table("lifts") {
    val id = long("id").autoIncrement()
    val type = enumerationByName<LiftType>("type", 20)
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id, name = "pk_lifts")
}

object Workouts : Table("workouts") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val startTime = timestamp("start_time").default(Instant.now())
    val endTime = timestamp("end_time").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id, name = "pk_workouts")
}

object WorkoutSets : Table("workout_sets") {
    val id = long("id").autoIncrement()
    val workoutId = long("workout_id").references(Workouts.id, onDelete = ReferenceOption.CASCADE)
    val liftId = long("lift_id").references(Lifts.id)
    val weight = double("weight")
    val reps = integer("reps")
    val rpe = double("rpe").nullable()
    val notes = text("notes").nullable()
    val completedAt = timestamp("completed_at").default(Instant.now())
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id, name = "pk_workout_sets")
}
