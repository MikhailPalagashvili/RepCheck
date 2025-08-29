package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.infrastructure.s3.S3ClientProvider
import com.repcheck.infrastructure.s3.S3UploadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.s3.S3Client
import java.net.URL
import java.util.*

class VideoService(
    private val videoRepository: VideoRepository,
    private val s3UploadService: S3UploadService,
    private val videoProcessor: VideoProcessor,
    private val s3Client: S3Client = S3ClientProvider.s3Client
) {
    /**
     * Creates a video record and returns a presigned URL for direct upload
     */
    fun createVideoAndGetUploadUrl(
        userId: Long,
        workoutSetId: Long,
        fileExtension: String = "mp4"
    ): Pair<WorkoutVideo, URL> {
        val videoId = UUID.randomUUID().toString()
        val video = videoRepository.create(
            userId = userId,
            workoutSetId = workoutSetId,
            s3Key = "videos/$videoId.$fileExtension",
            s3Bucket = s3UploadService.bucketName,
            status = VideoStatus.UPLOADING
        )
        val uploadUrl = s3UploadService.generateVideoUploadUrl(videoId, fileExtension)
        return video to uploadUrl
    }

    fun getVideo(videoId: Long): WorkoutVideo? = videoRepository.findById(videoId)

    fun findByS3Key(s3Key: String): WorkoutVideo? {
        return videoRepository.findByS3Key(s3Key)
    }

    suspend fun handleS3UploadEvent(videoId: Long, s3Key: String, bucket: String) {
        val video = videoRepository.findById(videoId) ?: return
        if (video.status != VideoStatus.UPLOADING) return

        withContext(Dispatchers.IO) {
            try {
                val response = s3Client.headObject {
                    it.bucket(bucket).key(s3Key)
                }
                videoRepository.updateFileSize(videoId, response.contentLength())
                processVideo(videoId)
            } catch (e: Exception) {
                videoRepository.updateStatus(videoId, VideoStatus.FAILED)
            }
        }
    }

    private suspend fun processVideo(videoId: Long) {
        try {
            val video = videoRepository.findById(videoId) ?: return
            videoRepository.updateStatus(videoId, VideoStatus.PROCESSING)
            videoProcessor(video)
            videoRepository.updateStatus(videoId, VideoStatus.PROCESSED)
        } catch (e: Exception) {
            videoRepository.updateStatus(videoId, VideoStatus.FAILED)
            // Consider adding error logging here
        }
    }
}