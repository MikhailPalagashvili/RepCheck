package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.infrastructure.queue.SqsQueueService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import software.amazon.awssdk.services.sqs.model.Message

class VideoProcessingWorkerTest : BehaviorSpec({

    val mockQueueService = mockk<SqsQueueService>()
    val mockVideoRepository = mockk<VideoRepository>()
    val mockVideoProcessor = mockk<VideoProcessor>()
    val mockProgressTracker = mockk<ProcessingProgressTracker>()

    val testVideo = WorkoutVideo(
        id = 1L,
        userId = 1L,
        workoutSetId = 1L,
        s3Key = "videos/1.mp4",
        s3Bucket = "test-bucket",
        status = VideoStatus.UPLOADED
    )

    val testMessage = Message.builder()
        .messageId("test-message")
        .body("1") // video ID
        .receiptHandle("test-receipt-handle")  // Add this line
        .build()

    beforeEach {
        clearAllMocks()
        coEvery { mockQueueService.receiveMessages() } returns listOf(testMessage)
        coEvery { mockVideoRepository.findById(1L) } returns testVideo
        coEvery { mockVideoProcessor.process(any()) } returns ProcessingResult(
            videoId = 1L,
            status = VideoStatus.PROCESSED
        )
        coEvery { mockProgressTracker.updateProgress(any(), any()) } just Runs
        coEvery { mockVideoRepository.update(any()) } returns true
        coEvery { mockVideoRepository.updateStatus(any(), any()) } returns true
        coEvery { mockProgressTracker.removeProgress(any()) } just Runs
        coEvery { mockQueueService.deleteMessage(any()) } just Awaits  // Add this line
        coEvery { mockQueueService.moveToDlq(any()) } just Runs      // Add this line
    }

    afterEach {
        unmockkAll()
    }

    given("VideoProcessingWorker") {
        `when`("started") {
            then("should begin polling the queue") {
                runTest {
                    val worker = VideoProcessingWorker(
                        queueService = mockQueueService,
                        videoRepository = mockVideoRepository,
                        videoProcessor = mockVideoProcessor,
                        progressTracker = mockProgressTracker,
                        pollingIntervalMs = 100
                    )

                    worker.start()
                    delay(300) // Wait for a few polling cycles
                    worker.stop()

                    coVerify(exactly = 1) { mockQueueService.receiveMessages() }
                }
            }

            then("should process messages from the queue") {
                runTest {
                    val worker = VideoProcessingWorker(
                        queueService = mockQueueService,
                        videoRepository = mockVideoRepository,
                        videoProcessor = mockVideoProcessor,
                        progressTracker = mockProgressTracker
                    )

                    worker.processMessage(testMessage)

                    coVerify(exactly = 1) { mockVideoProcessor.process(any()) }
                    coVerify(exactly = 1) { mockVideoRepository.update(any()) }
                    coVerify(exactly = 1) { mockProgressTracker.updateProgress(1L, 100) }
                }
            }

            then("should handle processing errors gracefully") {
                runTest {
                    val error = RuntimeException("Processing failed")
                    coEvery { mockVideoProcessor.process(any()) } throws error

                    val worker = VideoProcessingWorker(
                        queueService = mockQueueService,
                        videoRepository = mockVideoRepository,
                        videoProcessor = mockVideoProcessor,
                        progressTracker = mockProgressTracker
                    )

                    val result = runCatching {
                        worker.processMessage(testMessage)
                    }

                    result.isFailure shouldBe true
                    coVerify(exactly = 1) { mockVideoRepository.updateStatus(1L, VideoStatus.FAILED) }
                    coVerify(exactly = 1) { mockProgressTracker.removeProgress(1L) }
                }
            }

            then("should handle invalid message format") {
                runTest {
                    val invalidMessage = Message.builder()
                        .messageId("invalid")
                        .body("not-a-number")
                        .build()

                    val worker = VideoProcessingWorker(
                        queueService = mockQueueService,
                        videoRepository = mockVideoRepository,
                        videoProcessor = mockVideoProcessor,
                        progressTracker = mockProgressTracker
                    )

                    val result = runCatching {
                        worker.processMessage(invalidMessage)
                    }

                    result.isFailure shouldBe true
                    result.exceptionOrNull() shouldBe instanceOf<IllegalArgumentException>()
                }
            }

            then("should handle non-existent video") {
                runTest {
                    coEvery { mockVideoRepository.findById(999L) } returns null

                    val nonExistentMessage = Message.builder()
                        .messageId("non-existent")
                        .body("999")
                        .build()

                    val worker = VideoProcessingWorker(
                        queueService = mockQueueService,
                        videoRepository = mockVideoRepository,
                        videoProcessor = mockVideoProcessor,
                        progressTracker = mockProgressTracker
                    )

                    val result = runCatching {
                        worker.processMessage(nonExistentMessage)
                    }

                    result.isFailure shouldBe true
                    result.exceptionOrNull() shouldBe instanceOf<IllegalArgumentException>()
                }
            }
        }
    }
})
