package com.repcheck.features.video.domain.service.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

/**
 * Metrics for tracking video processing performance and reliability
 */
class VideoProcessingMetrics(private val registry: MeterRegistry) {
    // Timers
    private val processingTime = Timer.builder("video.processing.time")
        .description("Time taken to process a video")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry)

    // Counters
    private val processingSuccess = registry.counter("video.processing.success", "status", "success")
    private val processingErrors = registry.counter("video.processing.errors", "status", "error")
    private val retryAttempts = registry.counter("video.processing.retries")
    private val dlqMessages = registry.counter("video.processing.dlq.messages")
    private val queueDepth = registry.gauge("video.queue.depth", 0)

    /**
     * Record the time taken to process a video
     */
    fun recordProcessingTime(processingTimeMs: Long) {
        processingTime.record(processingTimeMs, TimeUnit.MILLISECONDS)
    }

    /**
     * Increment the success counter
     */
    fun incrementSuccess() {
        processingSuccess.increment()
    }

    /**
     * Increment the error counter
     */
    fun incrementError() {
        processingErrors.increment()
    }

    /**
     * Increment the retry counter
     */
    fun incrementRetry() {
        retryAttempts.increment()
    }

    /**
     * Increment the DLQ message counter
     */
    fun incrementDlqMessages() {
        dlqMessages.increment()
    }
}
