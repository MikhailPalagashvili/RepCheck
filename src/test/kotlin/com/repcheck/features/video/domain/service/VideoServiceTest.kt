package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.infrastructure.config.S3Config
import com.repcheck.infrastructure.s3.S3ClientProvider
import com.repcheck.infrastructure.s3.S3UploadService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.Duration

@ExperimentalCoroutinesApi
class VideoServiceTest {

    private val videoRepository = mockk<VideoRepository>(relaxed = true)
    private val s3UploadService = mockk<S3UploadService>(relaxed = true)
    private val videoProcessor = mockk<VideoProcessor>(relaxed = true)

    private lateinit var videoService: VideoService

    @BeforeEach
    fun setUp() {
        // Initialize S3ClientProvider with test config
        val testConfig = S3Config(
            bucketName = "test-bucket",
            presignedUrlExpiry = Duration.ofMinutes(15),
            region = "us-east-1"
        )
        S3ClientProvider.initialize(testConfig)

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
}
