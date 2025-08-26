package com.repcheck.domain.services

import com.repcheck.infrastructure.storage.S3Service
import java.time.Duration
import java.util.*

class VideoService(
    private val s3Service: S3Service
) {
    suspend fun generateUploadUrl(
        fileName: String,
        contentType: String = "video/mp4",
        durationMinutes: Long = 15
    ): String {
        val fileKey = "videos/${UUID.randomUUID()}/$fileName"
        return s3Service.generatePresignedUrl(
            key = fileKey,
            contentType = contentType,
            duration = Duration.ofMinutes(durationMinutes)
        ).toString()
    }

    suspend fun confirmUpload(fileKey: String): Boolean {
        return s3Service.objectExists(fileKey)
    }

    fun getVideoUrl(fileKey: String): String {
        return s3Service.getObjectUrl(fileKey).toString()
    }

    suspend fun deleteVideo(fileKey: String) {
        s3Service.deleteObject(fileKey)
    }

    fun extractKeyFromUrl(url: String): String {
        // Extract the key from a full S3 URL
        val uri = java.net.URI(url)
        return uri.path.substring(1) // Remove leading slash
    }
}
