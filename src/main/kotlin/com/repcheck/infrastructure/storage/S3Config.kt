package com.repcheck.infrastructure.storage

data class S3Config(
    val bucketName: String,
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val endpoint: String? = null
) {
    companion object {
        fun createFromEnv(): S3Config = S3Config(
            bucketName = System.getenv("AWS_S3_BUCKET") ?: "repcheck-videos",
            region = System.getenv("AWS_REGION") ?: "us-east-1",
            accessKeyId = requireNotNull(System.getenv("AWS_ACCESS_KEY_ID")) {
                "AWS_ACCESS_KEY_ID environment variable is required"
            },
            secretAccessKey = requireNotNull(System.getenv("AWS_SECRET_ACCESS_KEY")) {
                "AWS_SECRET_ACCESS_KEY environment variable is required"
            },
            endpoint = System.getenv("AWS_ENDPOINT")
        )
    }
}
