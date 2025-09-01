package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.repository.VideoRepository
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
    private val tempDir: Path = Path.of("/tmp/repcheck/videos"),
    private val pollingIntervalMs: Long = 5000L
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
                    logger.error(e) { "Error in worker loop" }
                }
                delay(pollingIntervalMs)
            }
        }
    }

    private suspend fun processNextMessage() {
        val messages = queueService.receiveMessages()
        if (messages.isEmpty()) return

        messages.forEach { message ->
            try {
                processMessage(message)
                queueService.deleteMessage(message.receiptHandle())
            } catch (e: Exception) {
                logger.error(e) { "Failed to process message: ${message.body()}" }
                queueService.moveToDlq(message)
            }
        }
    }

    internal suspend fun processMessage(message: Message) {
        val videoId = message.body().toLongOrNull()
            ?: throw IllegalArgumentException("Invalid message format: ${message.body()}")

        logger.info { "Processing video: $videoId" }
        val video = videoRepository.findById(videoId)
            ?: throw IllegalArgumentException("Video not found: $videoId")

        videoRepository.updateStatus(videoId, VideoStatus.PROCESSING)
        progressTracker.updateProgress(videoId, 10)

        try {
            // Process the video
            val result = videoProcessor.process(video)

            // Update video with results
            videoRepository.update(
                video.copy(
                    status = result.status,
                    updatedAt = System.currentTimeMillis()
                )
            )

            progressTracker.updateProgress(videoId, 100)
            logger.info { "Successfully processed video: $videoId" }
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