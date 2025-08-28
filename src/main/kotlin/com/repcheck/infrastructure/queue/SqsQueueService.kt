package com.repcheck.infrastructure.queue

import com.repcheck.infrastructure.config.SqsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

class SqsQueueService(
    private val sqsClient: SqsClient,
    private val config: SqsConfig
) {

    suspend fun receiveMessages(): List<Message> = withContext(Dispatchers.IO) {
        try {
            val request = ReceiveMessageRequest.builder()
                .queueUrl(config.queueUrl)
                .maxNumberOfMessages(config.maxNumberOfMessages)
                .waitTimeSeconds(config.waitTimeSeconds)
                .visibilityTimeout(config.visibilityTimeout.inWholeMilliseconds.toInt())
                .build()

            sqsClient.receiveMessage(request).messages()
        } catch (e: Exception) {
            throw QueueException("Failed to receive messages from queue", e)
        }
    }

    suspend fun deleteMessage(receiptHandle: String) = withContext(Dispatchers.IO) {
        try {
            val request = DeleteMessageRequest.builder()
                .queueUrl(config.queueUrl)
                .receiptHandle(receiptHandle)
                .build()

            sqsClient.deleteMessage(request)
        } catch (e: Exception) {
            throw QueueException("Failed to delete message from queue", e)
        }
    }

    suspend fun sendMessage(messageBody: String, messageGroupId: String? = null) = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = SendMessageRequest.builder()
                .queueUrl(config.queueUrl)
                .messageBody(messageBody)

            // Add message group ID for FIFO queues
            messageGroupId?.let {
                requestBuilder.messageGroupId(it)
            }

            sqsClient.sendMessage(requestBuilder.build())
        } catch (e: Exception) {
            throw QueueException("Failed to send message to queue", e)
        }
    }

    suspend fun moveToDlq(message: Message) {
        val dlqUrl = config.deadLetterQueueUrl ?: return
        try {
            val sendRequest = SendMessageRequest.builder()
                .queueUrl(dlqUrl)
                .messageBody(message.body())
                .build()
            sqsClient.sendMessage(sendRequest)
            deleteMessage(message.receiptHandle())
        } catch (e: Exception) {
            throw QueueException("Failed to move message to DLQ", e)
        }
    }
}

class QueueException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
