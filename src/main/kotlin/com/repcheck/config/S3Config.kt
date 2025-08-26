package com.repcheck.config

import io.ktor.server.config.*
import java.time.Duration

data class S3Config(
    val bucketName: String,
    val presignedUrlExpiry: Duration,
    val region: String
) {
    companion object {
        private const val DEFAULT_BUCKET_NAME = "repcheck-videos"
        private val DEFAULT_PRESIGNED_URL_EXPIRY = Duration.ofMinutes(15)
        private const val DEFAULT_REGION = "us-east-1"

        fun fromConfig(config: ApplicationConfig): S3Config {
            return try {
                // Check if aws config exists
                val awsConfig = try {
                    config.config("aws")
                } catch (e: ApplicationConfigurationException) {
                    return getDefaultConfig()
                }
                
                // Check if s3 config exists
                val s3Config = try {
                    awsConfig.config("s3")
                } catch (e: ApplicationConfigurationException) {
                    return getDefaultConfig()
                }
                
                // Get properties with fallbacks
                val bucketName = try {
                    s3Config.property("bucketName").getString()
                } catch (e: ApplicationConfigurationException) {
                    DEFAULT_BUCKET_NAME
                }
                
                val region = try {
                    awsConfig.property("region").getString()
                } catch (e: ApplicationConfigurationException) {
                    DEFAULT_REGION
                }
                
                val presignedUrlExpiry = try {
                    val durationString = s3Config.property("presignedUrlExpiry").getString()
                    Duration.parse("PT" + durationString.uppercase()
                        .replace(Regex("([A-Z])"), "$1")
                        .replace("MIN", "M")
                        .replace("HOUR", "H")
                        .replace("DAY", "D"))
                } catch (e: Exception) {
                    DEFAULT_PRESIGNED_URL_EXPIRY
                }
                
                S3Config(
                    bucketName = bucketName,
                    presignedUrlExpiry = presignedUrlExpiry,
                    region = region
                )
            } catch (e: Exception) {
                // If anything goes wrong, return default config
                getDefaultConfig()
            }
        }
        
        private fun getDefaultConfig(): S3Config {
            return S3Config(
                bucketName = DEFAULT_BUCKET_NAME,
                presignedUrlExpiry = DEFAULT_PRESIGNED_URL_EXPIRY,
                region = DEFAULT_REGION
            )
        }
    }
}
