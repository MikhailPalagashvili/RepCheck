package com.repcheck.features.video.domain.repository

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo

interface VideoRepository {
    fun create(
        userId: Long,
        workoutSetId: Long,
        s3Key: String,
        s3Bucket: String,
        status: VideoStatus = VideoStatus.UPLOADING,
    ): WorkoutVideo

    fun updateStatus(id: Long, status: VideoStatus): Boolean

    fun updateDuration(id: Long, durationSeconds: Int?): Boolean

    fun updateFileSize(id: Long, fileSizeBytes: Long?): Boolean

    fun attachToSet(id: Long, workoutSetId: Long): Boolean

    fun findById(id: Long): WorkoutVideo?

    fun findBySet(workoutSetId: Long): List<WorkoutVideo>

    fun findByUser(userId: Long, limit: Int = 50, offset: Long = 0): List<WorkoutVideo>
    
    fun findByS3Key(s3Key: String): WorkoutVideo?

    fun delete(id: Long): Boolean
}
