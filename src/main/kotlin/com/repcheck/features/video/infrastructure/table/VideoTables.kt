package com.repcheck.features.video.infrastructure.table

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.workout.infrastructure.table.WorkoutSets
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import org.jetbrains.exposed.dao.id.LongIdTable as IdTable

object WorkoutVideos : IdTable("workout_videos") {
    val userId = long("user_id")
    val workoutSetId = long("workout_set_id").references(
        WorkoutSets.id,
        onDelete = ReferenceOption.CASCADE
    )
    val s3Key = varchar("s3_key", 255)
    val s3Bucket = varchar("s3_bucket", 100)
    val status = enumerationByName<VideoStatus>("status", 20).default(VideoStatus.UPLOADING)
    val durationSeconds = integer("duration_seconds").nullable()
    val fileSizeBytes = long("file_size_bytes").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

