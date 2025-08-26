package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.infrastructure.s3.S3UploadService
import java.net.URL
import java.util.*

class VideoService(
    private val videoRepository: VideoRepository,
    private val s3UploadService: S3UploadService
) {

    /**
     * Creates a video record and returns a presigned URL for direct upload
     */
    fun createVideoAndGetUploadUrl(
        userId: Long,
        workoutSetId: Long,
        fileExtension: String = "mp4"
    ): Pair<WorkoutVideo, URL> {
        // Generate a unique videoId
        val videoId = UUID.randomUUID().toString()

        // Create video record in DB as UPLOADING
        val video = videoRepository.create(
            userId = userId,
            workoutSetId = workoutSetId,
            s3Key = "videos/$videoId.$fileExtension",
            s3Bucket = s3UploadService.bucketName,
            status = VideoStatus.UPLOADING
        )

        // Generate presigned URL
        val uploadUrl = s3UploadService.generateVideoUploadUrl(videoId, fileExtension)

        return video to uploadUrl
    }

    fun markVideoProcessed(videoId: Long, duration: Int, fileSize: Long) {
        videoRepository.updateStatus(videoId, VideoStatus.PROCESSED)
        videoRepository.updateDuration(videoId, duration)
        videoRepository.updateFileSize(videoId, fileSize)
    }

    fun getVideo(videoId: Long): WorkoutVideo? =
        videoRepository.findById(videoId)

    fun getVideosForUser(userId: Long, limit: Int = 50, offset: Long = 0): List<WorkoutVideo> =
        videoRepository.findByUser(userId, limit, offset)

    fun deleteVideo(videoId: Long): Boolean =
        videoRepository.delete(videoId)
}
