package com.repcheck.features.video.infrastructure.repository

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.features.video.infrastructure.table.WorkoutVideos
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ExposedVideoRepository : VideoRepository {
    private fun rowToDomain(row: ResultRow): WorkoutVideo = WorkoutVideo(
        id = row[WorkoutVideos.id].value,
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
        val id = WorkoutVideos.insertAndGetId { video ->
            video[WorkoutVideos.userId] = userId
            video[WorkoutVideos.workoutSetId] = workoutSetId
            video[WorkoutVideos.s3Key] = s3Key
            video[WorkoutVideos.s3Bucket] = s3Bucket
            video[WorkoutVideos.status] = status
        }

        WorkoutVideos.select { WorkoutVideos.id eq id }
            .single()
            .let(::rowToDomain)
    }

    override fun update(video: WorkoutVideo): Boolean = transaction {
        WorkoutVideos.update({ WorkoutVideos.id eq EntityID(video.id, WorkoutVideos) }) { st ->
            st[status] = video.status
            st[durationSeconds] = video.durationSeconds
            st[fileSizeBytes] = video.fileSizeBytes
            st[updatedAt] = Instant.now()
        } > 0
    }

    override fun updateStatus(id: Long, status: VideoStatus): Boolean = transaction {
        WorkoutVideos.update({ WorkoutVideos.id eq EntityID(id, WorkoutVideos) }) { video ->
            video[WorkoutVideos.status] = status
            video[WorkoutVideos.updatedAt] = Instant.now()
        } > 0
    }

    override fun updateDuration(id: Long, durationSeconds: Int?): Boolean = transaction {
        WorkoutVideos.update({ WorkoutVideos.id eq EntityID(id, WorkoutVideos) }) { st ->
            st[WorkoutVideos.durationSeconds] = durationSeconds
            st[WorkoutVideos.updatedAt] = Instant.now()
        } > 0
    }

    override fun updateFileSize(id: Long, fileSizeBytes: Long?): Boolean = transaction {
        WorkoutVideos.update({ WorkoutVideos.id eq EntityID(id, WorkoutVideos) }) { st ->
            st[WorkoutVideos.fileSizeBytes] = fileSizeBytes
            st[WorkoutVideos.updatedAt] = Instant.now()
        } > 0
    }

    override fun attachToSet(id: Long, workoutSetId: Long): Boolean = transaction {
        WorkoutVideos.update({ WorkoutVideos.id eq id }) { st ->
            st[WorkoutVideos.workoutSetId] = workoutSetId
        } > 0
    }

    override fun findById(id: Long): WorkoutVideo? = transaction {
        WorkoutVideos.select { WorkoutVideos.id eq EntityID(id, WorkoutVideos) }
            .map(::rowToDomain)
            .singleOrNull()
    }

    override fun findBySet(workoutSetId: Long): List<WorkoutVideo> = transaction {
        WorkoutVideos.select { WorkoutVideos.workoutSetId eq workoutSetId }
            .orderBy(WorkoutVideos.createdAt to SortOrder.DESC)
            .map(::rowToDomain)
    }

    override fun findByUser(userId: Long, limit: Int, offset: Long): List<WorkoutVideo> = transaction {
        WorkoutVideos.select { WorkoutVideos.userId eq userId }
            .orderBy(WorkoutVideos.createdAt to SortOrder.DESC)
            .limit(n = limit, offset = offset)
            .map(::rowToDomain)
    }

    override fun findByS3Key(s3Key: String): WorkoutVideo? {
        TODO("Not yet implemented")
    }

    override fun delete(id: Long): Boolean = transaction {
        WorkoutVideos.deleteWhere { WorkoutVideos.id eq EntityID(id, WorkoutVideos) } > 0
    }
}
