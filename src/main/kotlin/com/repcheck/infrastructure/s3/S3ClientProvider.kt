package com.repcheck.infrastructure.s3

import com.repcheck.infrastructure.config.S3Config
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

object S3ClientProvider {
    private lateinit var config: S3Config

    fun initialize(s3Config: S3Config) {
        config = s3Config
    }

    private val region: Region
        get() = Region.of(config.region)

    val s3Client: S3Client by lazy {
        S3Client.builder()
            .region(region)
            .build()
    }

    val s3Presigner: S3Presigner by lazy {
        S3Presigner.builder()
            .region(region)
            .build()
    }
}
