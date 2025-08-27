package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VideoProcessorTest {
    private val videoProcessor = defaultVideoProcessor

    @Test
    fun `processVideo should return video with PROCESSED status`() = runTest {
        // Given
        val video = WorkoutVideo(
            id = 1L,
            userId = 1L,
            workoutSetId = 1L,
            s3Key = "videos/test.mp4",
            s3Bucket = "test-bucket",
            status = VideoStatus.UPLOADED
        )

        // When
        val result = videoProcessor.invoke(video)

        // Then
        assertEquals(VideoStatus.PROCESSED, result.status)
        assertEquals(video.id, result.id)
        assertEquals(video.userId, result.userId)
        assertEquals(video.workoutSetId, result.workoutSetId)
    }

    @Test
    fun `processVideo should not modify other video properties`() = runTest {
        // Given
        val video = WorkoutVideo(
            id = 1L,
            userId = 1L,
            workoutSetId = 1L,
            s3Key = "videos/test.mp4",
            s3Bucket = "test-bucket",
            status = VideoStatus.UPLOADED,
            durationSeconds = 30,
            fileSizeBytes = 1024 * 1024 // 1MB
        )

        // When
        val result = videoProcessor.invoke(video)

        // Then
        assertEquals(video.durationSeconds, result.durationSeconds)
        assertEquals(video.fileSizeBytes, result.fileSizeBytes)
        assertEquals(video.s3Key, result.s3Key)
        assertEquals(video.s3Bucket, result.s3Bucket)
    }
}
