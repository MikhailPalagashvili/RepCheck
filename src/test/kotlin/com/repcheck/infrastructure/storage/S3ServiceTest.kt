package com.repcheck.infrastructure.storage

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL
import java.util.function.Consumer

class S3ServiceTest : StringSpec({
    val mockS3Client = mockk<S3Client>()
    val mockS3Presigner = mockk<S3Presigner>()
    val bucketName = "test-bucket"
    val s3Service = S3Service(
        mockS3Client,
        mockS3Presigner,
        S3Config(bucketName, "us-east-1", "test-access-key", "test-secret-key")
    )
    val testKey = "test-file.txt"
    val testContent = "test content".toByteArray()
    val testUrl = URL("https://test-bucket.s3.amazonaws.com/test-file.txt")

    "generatePresignedUrl should return a valid URL" {
        val presignedRequest = mockk<software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest>()
        every { presignedRequest.url() } returns testUrl

        every {
            mockS3Presigner.presignPutObject(any<Consumer<PutObjectPresignRequest.Builder>>())
        } returns presignedRequest

        val url = s3Service.generatePresignedUrl(testKey)

        url shouldBe testUrl
        verify { mockS3Presigner.presignPutObject(any<Consumer<PutObjectPresignRequest.Builder>>()) }
    }

    "objectExists should return true when object exists" {
        every {
            mockS3Client.headObject(any<HeadObjectRequest>())
        } returns mockk()

        val exists = s3Service.objectExists(testKey)

        exists shouldBe true
        verify { mockS3Client.headObject(any<HeadObjectRequest>()) }
    }

    "objectExists should return false when object doesn't exist" {
        val exception = S3Exception.builder()
            .statusCode(404)
            .message("Not Found")
            .build()

        every {
            mockS3Client.headObject(any<HeadObjectRequest>())
        } throws exception

        val exists = s3Service.objectExists(testKey)

        exists shouldBe false
    }

    "uploadObject should upload content to S3" {
        every {
            mockS3Client.putObject(any<Consumer<PutObjectRequest.Builder>>(), any<RequestBody>())
        } returns mockk()

        s3Service.uploadObject(testKey, testContent)

        verify {
            mockS3Client.putObject(
                any<Consumer<PutObjectRequest.Builder>>(),
                any<RequestBody>()
            )
        }
    }

    "deleteObject should delete object from S3" {
        every {
            mockS3Client.deleteObject(any<Consumer<DeleteObjectRequest.Builder>>())
        } returns mockk()

        s3Service.deleteObject(testKey)

        verify {
            mockS3Client.deleteObject(any<Consumer<DeleteObjectRequest.Builder>>())
        }
    }

    "getObjectUrl should return correct URL" {
        val url = s3Service.getObjectUrl(testKey)
        url.toString() shouldBe "https://$bucketName.s3.amazonaws.com/$testKey"
    }
})
