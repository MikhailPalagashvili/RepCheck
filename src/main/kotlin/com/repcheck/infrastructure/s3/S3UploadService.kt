package com.repcheck.infrastructure.s3

import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL
import java.time.Duration

class S3UploadService(
    private val bucketName: String,
    private val presignedUrlExpiry: Duration,
    private val s3Presigner: S3Presigner = S3ClientProvider.s3Presigner
) {
    fun generateVideoUploadUrl(
        videoId: String,
        fileExtension: String = "mp4",
        contentType: String = "video/mp4"
    ): URL {
        val objectKey = "videos/$videoId.$fileExtension"
        return generatePresignedUrl(objectKey, contentType)
    }

    private fun generatePresignedUrl(objectKey: String, contentType: String): URL {
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .contentType(contentType)
            .build()
        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(presignedUrlExpiry)
            .putObjectRequest(request)
            .build()
        return s3Presigner.presignPutObject(presignRequest).url()
    }
}
