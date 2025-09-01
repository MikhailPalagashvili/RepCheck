package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoAnalysis
import com.repcheck.features.video.domain.model.VideoProcessingProgress
import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class VideoProcessorTest {

    @MockK
    private lateinit var mockAnalysisService: VideoAnalysisService

    @MockK
    private lateinit var mockProgressTracker: ProcessingProgressTracker

    private lateinit var videoProcessor: VideoProcessor
    private val progressUpdates = mutableListOf<Int>()

    private val testVideo = WorkoutVideo(
        id = 1L,
        userId = 1L,
        workoutSetId = 1L,
        s3Key = "test/video.mp4",
        s3Bucket = "test-bucket",
        status = VideoStatus.UPLOADED
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        progressUpdates.clear()
        videoProcessor = DefaultVideoProcessor(mockAnalysisService, mockProgressTracker)
        coEvery { mockProgressTracker.updateProgress(any(), any()) } coAnswers {
            val progress = it.invocation.args[1] as Int
            progressUpdates.add(progress)
            Unit
        }
    }

    @Test
    fun processShouldCompleteSuccessfullyWithValidInput() = runBlocking {
        val testFeatures = VideoFeatures(
            duration = 60_000L,
            keyFrames = emptyList(),
            motionVectors = emptyList()
        )

        val testAnalysis = WorkoutAnalysis(
            exerciseType = "SQUAT",
            repetitions = 10,
            formScore = 0.95f,
            feedback = listOf("Good form!"),
            metrics = mapOf("knee_angle" to 170.0f)
        )

        val expectedAnalysis = VideoAnalysis(
            videoId = testVideo.id,
            analysisData = mockk(relaxed = true),
            confidence = testAnalysis.formScore
        )

        coEvery { mockAnalysisService.extractFeatures(testVideo) } returns testFeatures
        coEvery { mockAnalysisService.analyzeWorkout(testFeatures) } returns testAnalysis
        coEvery { mockAnalysisService.generateReport(testAnalysis) } returns expectedAnalysis

        val progressValues = mutableListOf<Int>()
        coEvery { mockProgressTracker.updateProgress(testVideo.id, capture(progressValues)) } just Runs

        val result = videoProcessor.process(testVideo)

        assertNotNull(result)
        assertEquals(testVideo.id, result.videoId)
        assertEquals(VideoStatus.PROCESSED, result.status)
        assertEquals(expectedAnalysis, result.analysis)

        coVerify(exactly = 9) {
            mockProgressTracker.updateProgress(testVideo.id, any())
        }

        assertEquals(0, progressValues[0])
        assertEquals(10, progressValues[1])
        assertEquals(20, progressValues[2])
        assertEquals(40, progressValues[3])
        assertEquals(50, progressValues[4])
        assertEquals(70, progressValues[5])
        assertEquals(80, progressValues[6])
        assertEquals(90, progressValues[7])
        assertEquals(100, progressValues[8])
    }

    @Test
    fun processShouldHandleAnalysisServiceFailure() = runTest {
        val errorMessage = "Analysis failed"
        coEvery { mockAnalysisService.extractFeatures(any()) } throws RuntimeException(errorMessage)
        val result = videoProcessor.process(testVideo)
        assertEquals(VideoStatus.FAILED, result.status)
        assertEquals(errorMessage, result.error)
        assertEquals(testVideo.id, result.videoId)
        coVerifyOrder {
            mockProgressTracker.updateProgress(testVideo.id, VideoProcessingProgress.INITIAL)
        }
    }

    @Test
    fun processShouldTrackProgressCorrectly() = runTest {
        // Given
        val testFeatures = VideoFeatures(
            duration = 60_000L,
            keyFrames = listOf(
                FrameAnalysis(
                    timestamp = 0L,
                    poseData = PoseData(emptyList(), 0.9f),
                    objectDetections = emptyList()
                )
            ),
            motionVectors = emptyList()
        )
        val testAnalysis = WorkoutAnalysis(
            exerciseType = "SQUAT",
            repetitions = 10,
            formScore = 0.95f,
            feedback = listOf("Good form!"),
            metrics = mapOf("key" to 1.0f)
        )
        val testReport = VideoAnalysis(
            videoId = testVideo.id,
            analysisData = JsonObject(emptyMap()),
            confidence = 0.95f
        )
        coEvery { mockAnalysisService.extractFeatures(testVideo) } returns testFeatures
        coEvery { mockAnalysisService.analyzeWorkout(testFeatures) } returns testAnalysis
        coEvery { mockAnalysisService.generateReport(testAnalysis) } returns testReport

        // When
        val result = videoProcessor.process(testVideo)

        // Then
        assertEquals(VideoStatus.PROCESSED, result.status)
        assertEquals(testVideo.id, result.videoId)

        // Verify we got at least 3 progress updates (one per major step)
        assertTrue(progressUpdates.size >= 3, "Expected at least 3 progress updates")

        // Verify progress is strictly increasing
        assertEquals(progressUpdates, progressUpdates.sorted(), "Progress should be non-decreasing")

        // Verify progress starts at 0 and ends at 100
        assertEquals(0, progressUpdates.first(), "Progress should start at 0%")
        assertEquals(100, progressUpdates.last(), "Progress should end at 100%")

        // Verify progress never goes below 0 or above 100
        assertTrue(progressUpdates.all { it in 0..100 }, "Progress should stay between 0 and 100")

        // Verify all expected service methods were called
        coVerify { mockAnalysisService.extractFeatures(testVideo) }
        coVerify { mockAnalysisService.analyzeWorkout(testFeatures) }
        coVerify { mockProgressTracker.updateProgress(testVideo.id, any()) }
    }

    @Test
    fun processShouldHandleAnalyzeWorkoutFailure() = runTest {
        // Given
        val testFeatures = VideoFeatures(
            duration = 60_000L,
            keyFrames = emptyList(),
            motionVectors = emptyList()
        )
        val errorMessage = "Analysis failed"

        coEvery { mockAnalysisService.extractFeatures(testVideo) } returns testFeatures
        coEvery { mockAnalysisService.analyzeWorkout(testFeatures) } throws RuntimeException(errorMessage)

        // When
        val result = videoProcessor.process(testVideo)

        // Then
        assertEquals(VideoStatus.FAILED, result.status)
        assertEquals(errorMessage, result.error)
        assertEquals(testVideo.id, result.videoId)

        // Verify progress was updated at least once
        coVerify(atLeast = 1) { mockProgressTracker.updateProgress(testVideo.id, any()) }

        // Verify extractFeatures was called but analyzeWorkout failed
        coVerify { mockAnalysisService.extractFeatures(testVideo) }
        coVerify { mockAnalysisService.analyzeWorkout(testFeatures) }

        // Verify generateReport was not called since analyzeWorkout failed
        coVerify(exactly = 0) { mockAnalysisService.generateReport(any<WorkoutAnalysis>()) }
    }

    @Test
    fun processShouldHandleReportGenerationFailure() = runTest {
        // Given
        val testFeatures = VideoFeatures(
            duration = 60_000L,
            keyFrames = emptyList(),
            motionVectors = emptyList()
        )
        val testAnalysis = WorkoutAnalysis(
            exerciseType = "SQUAT",
            repetitions = 10,
            formScore = 0.95f,
            feedback = listOf("Good form!"),
            metrics = mapOf("knee_angle" to 170.0f)
        )
        val errorMessage = "Report generation failed"

        coEvery { mockAnalysisService.extractFeatures(testVideo) } returns testFeatures
        coEvery { mockAnalysisService.analyzeWorkout(testFeatures) } returns testAnalysis
        coEvery { mockAnalysisService.generateReport(testAnalysis) } throws RuntimeException(errorMessage)

        // When
        val result = videoProcessor.process(testVideo)

        // Then
        assertEquals(VideoStatus.FAILED, result.status)
        assertEquals(errorMessage, result.error)
        assertEquals(testVideo.id, result.videoId)

        // Verify all service methods were called
        coVerify { mockAnalysisService.extractFeatures(testVideo) }
        coVerify { mockAnalysisService.analyzeWorkout(testFeatures) }
        coVerify { mockAnalysisService.generateReport(testAnalysis) }

        // Verify progress was updated
        coVerify(atLeast = 1) { mockProgressTracker.updateProgress(testVideo.id, any()) }
    }
}