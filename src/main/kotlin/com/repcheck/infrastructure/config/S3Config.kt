package com.repcheck.infrastructure.config

import io.ktor.server.config.*
import java.time.Duration

/**
 * Configuration for S3 storage
 */
data class S3Config(
    val bucketName: String,
    val presignedUrlExpiry: Duration,
    val region: String
) {
    companion object {
        private const val DEFAULT_BUCKET_NAME = "repcheck-videos"
        private val DEFAULT_PRESIGNED_URL_EXPIRY = Duration.ofMinutes(15)
        private const val DEFAULT_REGION = "us-east-1"
        private const val MIN_PRESIGNED_URL_EXPIRY_MINUTES = 1L
        private const val MAX_PRESIGNED_URL_EXPIRY_DAYS = 7L

        /**
         * Create S3Config from application configuration
         */
        fun fromConfig(config: ApplicationConfig): S3Config {
            return try {
                // Check if aws config exists
                val awsConfig = try {
                    config.config("aws")
                } catch (e: ApplicationConfigurationException) {
                    // If no aws config, use defaults
                    return S3Config(
                        bucketName = DEFAULT_BUCKET_NAME,
                        presignedUrlExpiry = DEFAULT_PRESIGNED_URL_EXPIRY,
                        region = DEFAULT_REGION
                    )
                }

                // Get presigned URL expiry from config or use default
                val presignedUrlExpiryMinutes = awsConfig
                    .propertyOrNull("s3.presignedUrlExpiryMinutes")
                    ?.getString()
                    ?.toLongOrNull()
                    ?.coerceIn(MIN_PRESIGNED_URL_EXPIRY_MINUTES, MAX_PRESIGNED_URL_EXPIRY_DAYS * 24 * 60)
                    ?: DEFAULT_PRESIGNED_URL_EXPIRY.toMinutes()

                S3Config(
                    bucketName = awsConfig.property("s3.bucketName").getString(),
                    presignedUrlExpiry = Duration.ofMinutes(presignedUrlExpiryMinutes),
                    region = awsConfig.propertyOrNull("s3.region")?.getString() ?: DEFAULT_REGION
                )
            } catch (e: Exception) {
                // Log the error and use defaults
                System.err.println("Error loading S3 config: ${e.message}. Using default configuration.")
                S3Config(
                    bucketName = DEFAULT_BUCKET_NAME,
                    presignedUrlExpiry = DEFAULT_PRESIGNED_URL_EXPIRY,
                    region = DEFAULT_REGION
                )
            }
        }
    }
}
