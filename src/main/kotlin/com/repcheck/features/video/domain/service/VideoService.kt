package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.infrastructure.s3.S3ClientProvider
import com.repcheck.infrastructure.s3.S3UploadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import software.amazon.awssdk.services.s3.S3Client
import java.net.URL

class VideoService(
    private val videoRepository: VideoRepository,
    private val s3UploadService: S3UploadService,
    private val videoProcessor: VideoProcessor,
    private val s3Client: S3Client = S3ClientProvider.s3Client
) : KoinComponent {
    private val logger = org.slf4j.LoggerFactory.getLogger(VideoService::class.java)

    fun createVideoAndGetUploadUrl(
        userId: Long,
        workoutSetId: Long,
        fileExtension: String = "mp4"
    ): Pair<WorkoutVideo, URL> {
        val videoId = System.currentTimeMillis() // Using timestamp as ID
        val videoKey = "videos/$videoId.$fileExtension"
        val video = videoRepository.create(
            userId = userId,
            workoutSetId = workoutSetId,
            s3Key = videoKey,
            s3Bucket = s3UploadService.bucketName,
            status = VideoStatus.UPLOADING
        )
        val uploadUrl = s3UploadService.generateVideoUploadUrl(videoId, fileExtension)
        return video to uploadUrl
    }

    suspend fun handleS3UploadEvent(videoId: Long, key: String, bucket: String) {
        try {
            videoRepository.updateStatus(videoId, VideoStatus.PROCESSING)
            val video = videoRepository.findById(videoId) ?: throw IllegalStateException("Video not found: $videoId")
            withContext(Dispatchers.IO) {
                try {
                    videoProcessor.process(video)
                    videoRepository.updateStatus(videoId, VideoStatus.PROCESSED)
                } catch (e: Exception) {
                    logger.error("Failed to process video: $videoId", e)
                    videoRepository.updateStatus(videoId, VideoStatus.FAILED)
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling S3 upload event", e)
            videoRepository.updateStatus(videoId, VideoStatus.FAILED)
        }
    }

    fun getVideo(videoId: Long): WorkoutVideo? = videoRepository.findById(videoId)

    fun findByS3Key(s3Key: String): WorkoutVideo? {
        return videoRepository.findByS3Key(s3Key)
    }
}