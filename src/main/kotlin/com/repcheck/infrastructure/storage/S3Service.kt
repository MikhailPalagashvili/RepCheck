package com.repcheck.infrastructure.storage

import com.repcheck.infrastructure.storage.S3Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL
import java.time.Duration

/**
 * Service for interacting with S3 storage
 * @property s3Client The S3 client for direct operations
 * @property s3Presigner The S3 presigner for generating pre-signed URLs
 * @property config The S3 configuration
 */
class S3Service(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val config: S3Config
) {
    private val bucketName = config.bucketName

    /**
     * Generates a pre-signed URL for uploading a file to S3
     * @param key The S3 object key
     * @param contentType The content type of the file
     * @param duration How long the URL should be valid
     * @return A pre-signed URL for uploading the file
     */
    suspend fun generatePresignedUrl(
        key: String, 
        contentType: String = "application/octet-stream", 
        duration: Duration = Duration.ofMinutes(15)
    ): URL = withContext(Dispatchers.IO) {
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(duration)
            .putObjectRequest(request)
            .build()

        s3Presigner.presignPutObject(presignRequest).url()
    }

    /**
     * Checks if an object exists in S3
     * @param key The S3 object key
     * @return true if the object exists, false otherwise
     */
    suspend fun objectExists(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            s3Client.headObject { it.bucket(bucketName).key(key) }
            true
        } catch (e: NoSuchKeyException) {
            false
        } catch (e: NoSuchBucketException) {
            false
        }
    }

    /**
     * Uploads a file to S3
     * @param key The S3 object key
     * @param content The file content as a byte array
     * @param contentType The content type of the file
     */
    suspend fun uploadObject(
        key: String, 
        content: ByteArray, 
        contentType: String = "application/octet-stream"
    ) = withContext(Dispatchers.IO) {
        s3Client.putObject(
            { it.bucket(bucketName).key(key).contentType(contentType) },
            RequestBody.fromBytes(content)
        )
    }

    /**
     * Deletes an object from S3
     * @param key The S3 object key to delete
     */
    suspend fun deleteObject(key: String) = withContext(Dispatchers.IO) {
        s3Client.deleteObject { it.bucket(bucketName).key(key) }
    }

    /**
     * Gets the public URL for an S3 object
     * @param key The S3 object key
     * @return The public URL for the object
     */
    fun getObjectUrl(key: String): URL {
        return s3Client.utilities()
            .getUrl { it.bucket(bucketName).key(key) }
    }
}
