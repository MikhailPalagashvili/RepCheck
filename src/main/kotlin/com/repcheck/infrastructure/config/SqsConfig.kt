package com.repcheck.infrastructure.config

import io.ktor.server.config.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class SqsConfig(
    val queueUrl: String,
    val visibilityTimeout: Duration = 30.seconds,
    val maxNumberOfMessages: Int = 10,
    val waitTimeSeconds: Int = 20,
    val deadLetterQueueUrl: String? = null
) {
    companion object {
        private const val DEFAULT_QUEUE_NAME = "video-processing-queue"
        private const val DEFAULT_REGION = "us-east-1"

        fun fromConfig(config: ApplicationConfig): SqsConfig {
            return try {
                val awsConfig = try {
                    config.config("aws")
                } catch (e: ApplicationConfigurationException) {
                    return SqsConfig(
                        queueUrl = "https://sqs.${DEFAULT_REGION}.amazonaws.com/000000000000/${DEFAULT_QUEUE_NAME}",
                        deadLetterQueueUrl = "https://sqs.${DEFAULT_REGION}.amazonaws.com/000000000000/${DEFAULT_QUEUE_NAME}-dlq"
                    )
                }

                SqsConfig(
                    queueUrl = awsConfig.property("sqs.queueUrl").getString(),
                    visibilityTimeout = awsConfig.propertyOrNull("sqs.visibilityTimeout")?.getString()
                        ?.toLongOrNull()?.seconds ?: 30.seconds,
                    maxNumberOfMessages = awsConfig.propertyOrNull("sqs.maxMessages")?.getString()?.toIntOrNull() ?: 10,
                    waitTimeSeconds = awsConfig.propertyOrNull("sqs.waitTimeSeconds")?.getString()?.toIntOrNull() ?: 20,
                    deadLetterQueueUrl = awsConfig.propertyOrNull("sqs.deadLetterQueueUrl")?.getString()
                )
            } catch (e: Exception) {
                System.err.println("Error loading SQS config: ${e.message}. Using default configuration.")
                SqsConfig(
                    queueUrl = "https://sqs.${DEFAULT_REGION}.amazonaws.com/000000000000/${DEFAULT_QUEUE_NAME}",
                    deadLetterQueueUrl = "https://sqs.${DEFAULT_REGION}.amazonaws.com/000000000000/${DEFAULT_QUEUE_NAME}-dlq"
                )
            }
        }
    }
}
