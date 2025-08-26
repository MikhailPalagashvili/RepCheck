package com.repcheck.features.video.presentation.dto

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import kotlinx.serialization.Serializable

@Serializable
data class UploadCompleteRequest(
    val videoId: Long,
    val fileSizeBytes: Long,
    val durationSeconds: Int
)

@Serializable
data class VideoResponse(
    val id: Long,
    val status: VideoStatus,
    val s3Key: String,
    val s3Bucket: String,
    val fileSizeBytes: Long?,
    val durationSeconds: Int?,
    val createdAt: Long,
    val updatedAt: Long
)

fun WorkoutVideo.toResponse() = VideoResponse(
    id = id,
    status = status,
    s3Key = s3Key,
    s3Bucket = s3Bucket,
    fileSizeBytes = fileSizeBytes,
    durationSeconds = durationSeconds,
    createdAt = createdAt,
    updatedAt = updatedAt
)
