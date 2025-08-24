package com.repcheck.features.video.infrastructure.db.repository

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.features.video.infrastructure.db.tables.WorkoutVideos
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ExposedVideoRepository : VideoRepository {
    private fun rowToDomain(row: ResultRow): WorkoutVideo = WorkoutVideo(
        id = row[WorkoutVideos.id],
        userId = row[WorkoutVideos.userId],
        workoutSetId = row[WorkoutVideos.workoutSetId],
        s3Key = row[WorkoutVideos.s3Key],
        s3Bucket = row[WorkoutVideos.s3Bucket],
        status = row[WorkoutVideos.status],
        durationSeconds = row[WorkoutVideos.durationSeconds],
        fileSizeBytes = row[WorkoutVideos.fileSizeBytes],
        createdAt = row[WorkoutVideos.createdAt].toEpochMilli(),
        updatedAt = row[WorkoutVideos.updatedAt].toEpochMilli(),
    )

    override fun create(
        userId: Long,
        workoutSetId: Long,
        s3Key: String,
        s3Bucket: String,
        status: VideoStatus,
    ): WorkoutVideo = transaction {
        val id = WorkoutVideos.insert { st ->
            st[WorkoutVideos.userId] = userId
            st[WorkoutVideos.workoutSetId] = workoutSetId
            st[WorkoutVideos.s3Key] = s3Key
            st[WorkoutVideos.s3Bucket] = s3Bucket
            st[WorkoutVideos.status] = status
        }[WorkoutVideos.id]

        WorkoutVideos.select { WorkoutVideos.id eq id }
            .single()
            .let(::rowToDomain)
    }

    override fun updateStatus(id: Long, status: VideoStatus): Boolean = transaction {
        WorkoutVideos.update({ WorkoutVideos.id eq id }) { st ->
            st[WorkoutVideos.status] = status
        } > 0
    }

    override fun updateDuration(id: Long, durationSeconds: Int?): Boolean = transaction {
        WorkoutVideos.update({ WorkoutVideos.id eq id }) { st ->
            st[WorkoutVideos.durationSeconds] = durationSeconds
        } > 0
    }

    override fun updateFileSize(id: Long, fileSizeBytes: Long?): Boolean = transaction {
        WorkoutVideos.update({ WorkoutVideos.id eq id }) { st ->
            st[WorkoutVideos.fileSizeBytes] = fileSizeBytes
        } > 0
    }

    override fun attachToSet(id: Long, workoutSetId: Long): Boolean = transaction {
        WorkoutVideos.update({ WorkoutVideos.id eq id }) { st ->
            st[WorkoutVideos.workoutSetId] = workoutSetId
        } > 0
    }

    override fun findById(id: Long): WorkoutVideo? = transaction {
        WorkoutVideos.select { WorkoutVideos.id eq id }
            .limit(1)
            .singleOrNull()
            ?.let(::rowToDomain)
    }

    override fun findBySet(workoutSetId: Long): List<WorkoutVideo> = transaction {
        WorkoutVideos.select { WorkoutVideos.workoutSetId eq workoutSetId }
            .orderBy(WorkoutVideos.createdAt to SortOrder.DESC)
            .map(::rowToDomain)
    }

    override fun findByUser(userId: Long, limit: Int, offset: Long): List<WorkoutVideo> = transaction {
        WorkoutVideos.select { WorkoutVideos.userId eq userId }
            .orderBy(WorkoutVideos.createdAt to SortOrder.DESC)
            .limit(limit, offset)
            .map(::rowToDomain)
    }

    override fun delete(id: Long): Boolean = transaction {
        WorkoutVideos.deleteWhere { WorkoutVideos.id eq id } > 0
    }
}
