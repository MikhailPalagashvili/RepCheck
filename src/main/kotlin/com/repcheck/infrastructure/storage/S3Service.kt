package com.repcheck.infrastructure.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL
import java.time.Duration

class S3Service(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val config: S3Config
) {

    suspend fun generatePresignedUrl(key: String): URL = withContext(Dispatchers.IO) {
        val request = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .putObjectRequest {
                it.bucket(config.bucketName)
                it.key(key)
            }
            .build()

        s3Presigner.presignPutObject(request).url()
    }

    suspend fun objectExists(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            s3Client.headObject {
                it.bucket(config.bucketName)
                it.key(key)
            }
            true
        } catch (e: S3Exception) {
            if (e.statusCode() == 404) false else throw e
        }
    }

    suspend fun uploadObject(key: String, content: ByteArray) = withContext(Dispatchers.IO) {
        s3Client.putObject(
            { it.bucket(config.bucketName).key(key) },
            RequestBody.fromBytes(content)
        )
    }

    suspend fun deleteObject(key: String) = withContext(Dispatchers.IO) {
        s3Client.deleteObject {
            it.bucket(config.bucketName)
            it.key(key)
        }
    }

    fun getObjectUrl(key: String): URL =
        s3Client.utilities().getUrl {
            it.bucket(config.bucketName)
            it.key(key)
        }
}
