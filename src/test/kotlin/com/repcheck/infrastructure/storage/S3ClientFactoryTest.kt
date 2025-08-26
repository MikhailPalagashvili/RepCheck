package com.repcheck.infrastructure.storage

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class S3ClientFactoryTest : StringSpec({
    val testConfig = S3Config(
        bucketName = "test-bucket",
        region = "us-east-1",
        accessKeyId = "test-access-key",
        secretAccessKey = "test-secret-key"
    )

    "createS3Client should return a valid S3Client" {
        val s3Client = S3ClientFactory.createS3Client(testConfig)
        s3Client shouldNotBe null
        s3Client.serviceName() shouldBe "S3"
    }

    "createS3Presigner should return a valid S3Presigner" {
        val presigner = S3ClientFactory.createS3Presigner(testConfig)
        presigner shouldNotBe null
    }

    "createS3Client with custom endpoint should work" {
        val configWithEndpoint = testConfig.copy(endpoint = "http://localhost:4566")
        val s3Client = S3ClientFactory.createS3Client(configWithEndpoint)
        s3Client shouldNotBe null
    }

    "createS3Presigner with custom endpoint should work" {
        val configWithEndpoint = testConfig.copy(endpoint = "http://localhost:4566")
        val presigner = S3ClientFactory.createS3Presigner(configWithEndpoint)
        presigner shouldNotBe null
    }
})
