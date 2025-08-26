package com.repcheck.infrastructure.storage

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

object S3ClientFactory {
    /**
     * Creates a new S3 client with the provided configuration
     */
    fun createS3Client(config: S3Config): S3Client {
        val builder = S3Client.builder()
            .region(Region.of(config.region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey)
                )
            )

        // Add custom endpoint if provided (useful for local testing with LocalStack)
        config.endpoint?.let { builder.endpointOverride(URI.create(it)) }
        
        return builder.build()
    }

    /**
     * Creates a new S3 presigner with the provided configuration
     */
    fun createS3Presigner(config: S3Config): S3Presigner {
        val builder = S3Presigner.builder()
            .region(Region.of(config.region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey)
                )
            )

        // Add custom endpoint if provided (useful for local testing with LocalStack)
        config.endpoint?.let { builder.endpointOverride(URI.create(it)) }
        
        return builder.build()
    }
}
