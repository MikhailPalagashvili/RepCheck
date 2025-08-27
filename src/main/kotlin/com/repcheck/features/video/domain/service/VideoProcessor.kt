package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.domain.model.WorkoutVideo
import kotlinx.coroutines.delay

/**
 * Type alias for a function that processes a video and returns the updated video
 */
typealias VideoProcessor = suspend (WorkoutVideo) -> WorkoutVideo

/**
 * Default implementation of VideoProcessor that simulates video processing
 */
val defaultVideoProcessor: VideoProcessor = { video ->
    // Simulate processing time
    delay(2000)

    // Return the video with updated status
    video.copy(
        status = VideoStatus.PROCESSED
    )
}