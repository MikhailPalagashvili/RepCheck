package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.infrastructure.s3.S3UploadService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URL

@ExperimentalCoroutinesApi
class VideoServiceTest {

    private val videoRepository = mockk<VideoRepository>(relaxed = true)
    private val s3UploadService = mockk<S3UploadService>(relaxed = true)
    private val videoProcessor = mockk<VideoProcessor>(relaxed = true)

    private lateinit var videoService: VideoService

    @BeforeEach
    fun setUp() {
        videoService = VideoService(videoRepository, s3UploadService, videoProcessor)
    }

    @Test
    fun `createVideoAndGetUploadUrl should create video and return upload URL`() {
        // Given
        val userId = 1L
        val workoutSetId = 10L
        val fileExtension = "mp4"
        val uploadUrl = URL("https://test-bucket.s3.amazonaws.com/videos/test.mp4")
        val video = WorkoutVideo(
            id = 1L,
            userId = userId,
            workoutSetId = workoutSetId,
            s3Key = "videos/test.$fileExtension",
            s3Bucket = "test-bucket",
            status = VideoStatus.UPLOADING
        )

        every { s3UploadService.bucketName } returns "test-bucket"
        every { s3UploadService.generateVideoUploadUrl(any(), fileExtension) } returns uploadUrl
        every { videoRepository.create(any(), any(), any(), any(), any()) } returns video

        // When
        val (resultVideo, resultUrl) = videoService.createVideoAndGetUploadUrl(userId, workoutSetId, fileExtension)

        // Then
        Assertions.assertEquals(video, resultVideo)
        Assertions.assertEquals(uploadUrl, resultUrl)

        verify { s3UploadService.generateVideoUploadUrl(any(), fileExtension) }
        verify { videoRepository.create(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getVideo should return video from repository`() {
        // Given
        val videoId = 1L
        val video = mockk<WorkoutVideo>()
        every { videoRepository.findById(videoId) } returns video

        // When
        val result = videoService.getVideo(videoId)

        // Then
        Assertions.assertEquals(video, result)
        verify { videoRepository.findById(videoId) }
    }

    @Test
    fun `completeVideoUpload should update metadata, status, and process video`() = runTest {
        // Given
        val videoId = 1L
        val fileSize = 1024L
        val duration = 30
        val video = WorkoutVideo(
            id = videoId,
            userId = 1L,
            workoutSetId = 1L,
            s3Key = "videos/test.mp4",
            s3Bucket = "test-bucket",
            status = VideoStatus.UPLOADING
        )

        coEvery { videoRepository.updateFileSize(videoId, fileSize) } returns true
        coEvery { videoRepository.updateDuration(videoId, duration) } returns true
        coEvery { videoRepository.updateStatus(videoId, any()) } returns true
        coEvery { videoRepository.findById(videoId) } returns video
        coEvery { videoProcessor(video) } returns video

        // When
        val result = videoService.completeVideoUpload(videoId, fileSize, duration)

        // Then
        Assertions.assertNotNull(result)
        Assertions.assertEquals(videoId, result?.id)

        coVerifySequence {
            videoRepository.updateFileSize(videoId, fileSize)
            videoRepository.updateDuration(videoId, duration)
            videoRepository.updateStatus(videoId, VideoStatus.UPLOADED)
            videoRepository.findById(videoId)
            videoRepository.updateStatus(videoId, VideoStatus.PROCESSING)
            videoProcessor(video)
            videoRepository.updateStatus(videoId, VideoStatus.PROCESSED)
        }
    }

    @Test
    fun `completeVideoUpload should mark FAILED if processing throws exception`() = runTest {
        // Given
        val videoId = 1L
        val fileSize = 1024L
        val duration = 30
        val video = WorkoutVideo(
            id = videoId,
            userId = 1L,
            workoutSetId = 1L,
            s3Key = "videos/test.mp4",
            s3Bucket = "test-bucket",
            status = VideoStatus.UPLOADING
        )

        coEvery { videoRepository.updateFileSize(videoId, fileSize) } returns true
        coEvery { videoRepository.updateDuration(videoId, duration) } returns true
        coEvery { videoRepository.updateStatus(videoId, any()) } returns true
        coEvery { videoRepository.findById(videoId) } returns video
        coEvery { videoProcessor(video) } throws RuntimeException("Processing error")

        // When
        videoService.completeVideoUpload(videoId, fileSize, duration)

        // Then
        coVerify { videoRepository.updateStatus(videoId, VideoStatus.FAILED) }
    }
}
