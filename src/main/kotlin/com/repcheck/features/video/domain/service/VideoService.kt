package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.infrastructure.s3.S3UploadService
import java.net.URL
import java.util.*

class VideoService(
    private val videoRepository: VideoRepository,
    private val s3UploadService: S3UploadService,
    private val videoProcessor: VideoProcessor
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

    /**
     * Complete the video upload process by updating metadata and status
     * Note: This method doesn't use a transaction as the repository methods handle their own transactions
     * @param videoId ID of the video to update
     * @param fileSizeBytes Size of the uploaded file in bytes
     * @param durationSeconds Duration of the video in seconds
     * @return The updated video or null if not found
     */
    suspend fun completeVideoUpload(
        videoId: Long,
        fileSizeBytes: Long,
        durationSeconds: Int
    ): WorkoutVideo? {
        // Update video metadata
        videoRepository.updateFileSize(videoId, fileSizeBytes)
        videoRepository.updateDuration(videoId, durationSeconds)
        videoRepository.updateStatus(videoId, VideoStatus.UPLOADED)
        
        // Get the updated video
        val video = videoRepository.findById(videoId) ?: return null
        
        // Start processing the video asynchronously
        processVideoInBackground(video)
        
        return video
    }
    
    private suspend fun processVideoInBackground(video: WorkoutVideo) {
        try {
            // Update status to PROCESSING
            videoRepository.updateStatus(video.id, VideoStatus.PROCESSING)
            
            // Process the video by invoking the function
            val processedVideo = videoProcessor(video)
            
            // Update status to PROCESSED
            videoRepository.updateStatus(processedVideo.id, VideoStatus.PROCESSED)
            
        } catch (e: Exception) {
            // Update status to FAILED if there's an error
            videoRepository.updateStatus(video.id, VideoStatus.FAILED)
            // TODO: Add error logging
            e.printStackTrace()
        }
    }
}
