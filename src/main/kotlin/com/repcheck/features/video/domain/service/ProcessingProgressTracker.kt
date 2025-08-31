package com.repcheck.features.video.domain.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ProcessingProgressTracker {
    suspend fun updateProgress(videoId: Long, progress: Int)
    suspend fun getProgress(videoId: Long): Int?
    suspend fun removeProgress(videoId: Long)
}

class InMemoryProgressTracker : ProcessingProgressTracker {
    private val progressMap = mutableMapOf<Long, Int>()
    private val mutex = Mutex()

    override suspend fun updateProgress(videoId: Long, progress: Int) {
        mutex.withLock {
            progressMap[videoId] = progress.coerceIn(0, 100)
        }
    }

    override suspend fun getProgress(videoId: Long): Int? {
        return mutex.withLock {
            progressMap[videoId]
        }
    }

    override suspend fun removeProgress(videoId: Long) {
        mutex.withLock {
            progressMap.remove(videoId)
        }
    }
}