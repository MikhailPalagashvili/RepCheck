package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.repository.VideoRepository
import com.repcheck.features.video.domain.service.metrics.VideoProcessingMetrics
import com.repcheck.infrastructure.queue.SqsQueueService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import software.amazon.awssdk.services.sqs.model.Message
import java.nio.file.Path
import kotlin.io.path.createDirectories

class VideoProcessingWorker(
    private val queueService: SqsQueueService,
    private val videoRepository: VideoRepository,
    private val videoProcessor: VideoProcessor,
    private val progressTracker: ProcessingProgressTracker,
    private val metrics: VideoProcessingMetrics,
    private val tempDir: Path = Path.of("/tmp/repcheck/videos"),
    private val pollingIntervalMs: Long = 5000L,
    private val maxRetryAttempts: Int = 3,
    private val initialBackoffMs: Long = 1000L
) {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    fun start() {
        if (isRunning) {
            logger.warn { "Worker is already running" }
            return
        }

        isRunning = true
        logger.info { "Starting VideoProcessingWorker" }
        tempDir.createDirectories()

        scope.launch {
            while (isRunning) {
                try {
                    processNextMessage()
                } catch (e: Exception) {
                    logger.error(e) { "Unexpected error in worker loop" }
                }
                delay(pollingIntervalMs)
            }
        }
    }

    private suspend fun processNextMessage() {
        try {
            val messages = queueService.receiveMessages()
            if (messages.isEmpty()) return

            messages.forEach { message ->
                try {
                    processWithRetry(message)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to process message after retries: ${message.body()}" }
                    handleFailedMessage(message, e)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error receiving messages from queue" }
        }
    }

    private suspend fun processWithRetry(message: Message, attempt: Int = 1) {
        try {
            processMessage(message)
            queueService.deleteMessage(message.receiptHandle())
        } catch (e: Exception) {
            metrics.incrementRetry()
            if (attempt >= maxRetryAttempts) {
                metrics.incrementError()
                throw e // Rethrow to be handled by the caller
            }
            
            val backoffMs = initialBackoffMs * (1L shl (attempt - 1)) // Exponential backoff
            logger.warn(e) { "Attempt $attempt failed, retrying in ${backoffMs}ms" }
            
            delay(backoffMs)
            processWithRetry(message, attempt + 1)
        }
    }

    private suspend fun handleFailedMessage(message: Message, error: Throwable) {
        try {
            // Try to extract video ID for better error tracking
            val videoId = message.body().toLongOrNull()
            if (videoId != null) {
                videoRepository.updateStatus(videoId, VideoStatus.FAILED)
                progressTracker.removeProgress(videoId)
            }
            
            // Move to DLQ or handle the failure appropriately
            queueService.moveToDlq(message)
            metrics.incrementDlqMessages()
            
            logger.error(error) {
                "Moved message to DLQ. Body: ${message.body()}, " +
                "MessageId: ${message.messageId()}, " +
                "Error: ${error.message}"
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to handle failed message: ${message.messageId()}" }
        }
    }

    internal suspend fun processMessage(message: Message) {
        val videoId = message.body().toLongOrNull()
            ?: throw IllegalArgumentException("Invalid message format: ${message.body()}")

        logger.info { "Processing video: $videoId" }
        val video = videoRepository.findById(videoId)
            ?: throw IllegalArgumentException("Video not found: $videoId")

        val startTime = System.currentTimeMillis()
        try {
            videoRepository.updateStatus(videoId, VideoStatus.PROCESSING)
            progressTracker.updateProgress(videoId, 10)

            // Process the video
            val result = videoProcessor.process(video)

            // Update video with results
            videoRepository.update(
                video.copy(
                    status = result.status,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Record successful processing time
            val processingTime = System.currentTimeMillis() - startTime
            metrics.recordProcessingTime(processingTime)
            metrics.incrementSuccess()
            
            progressTracker.updateProgress(videoId, 100)
            logger.info { "Successfully processed video: $videoId in ${processingTime}ms" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process video: $videoId" }
            videoRepository.updateStatus(videoId, VideoStatus.FAILED)
            progressTracker.removeProgress(videoId)
            throw e
        }
    }

    fun stop() {
        logger.info { "Stopping VideoProcessingWorker" }
        isRunning = false
        scope.cancel()
    }
}