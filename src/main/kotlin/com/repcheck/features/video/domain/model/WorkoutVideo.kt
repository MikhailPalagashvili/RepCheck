package com.repcheck.features.video.domain.model

data class WorkoutVideo(
    val id: Long = 0,
    val userId: Long,
    val workoutSetId: Long,
    val s3Key: String,
    val s3Bucket: String,
    val status: VideoStatus = VideoStatus.UPLOADING,
    val durationSeconds: Int? = null,
    val fileSizeBytes: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class VideoStatus {
    UPLOADING,
    UPLOADED,
    PROCESSING,
    PROCESSED,
    FAILED
}
