package com.repcheck.features.video.presentation

import com.repcheck.common.exceptions.BadRequestException
import com.repcheck.common.exceptions.ForbiddenException
import com.repcheck.common.exceptions.NotFoundException
import com.repcheck.common.extensions.getUserId
import com.repcheck.features.video.domain.service.VideoService
import com.repcheck.features.video.presentation.dto.S3EventNotification
import com.repcheck.features.video.presentation.dto.toResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.videoRoutes(videoService: VideoService) = route("/videos") {
    authenticate("auth-jwt") {
        post("/presign") {
            val userId = call.getUserId()
            val workoutSetId = call.parameters["workoutSetId"]?.toLongOrNull()
                ?: throw BadRequestException("Missing workoutSetId")

            val fileExtension = call.parameters["fileExtension"] ?: "mp4"
            val (video, uploadUrl) = videoService.createVideoAndGetUploadUrl(
                userId = userId,
                workoutSetId = workoutSetId,
                fileExtension = fileExtension
            )

            call.respond(
                mapOf(
                    "videoId" to video.id,
                    "fileExtension" to fileExtension,
                    "uploadUrl" to uploadUrl.toString()
                )
            )
        }
        get("/{id}") {
            val userId = call.getUserId()
            val videoId = call.parameters["id"]?.toLongOrNull() ?: throw BadRequestException("Invalid video ID")
            val video = videoService.getVideo(videoId)
                ?: throw NotFoundException("Video not found")
            if (video.userId != userId) {
                throw ForbiddenException("Not authorized to access this video")
            }
            call.respond(video.toResponse())
        }
    }
    // S3 webhook doesn't require authentication
    post("/webhook/s3-events") {
        handleS3Webhook(call, videoService)
    }
}

private suspend fun handleS3Webhook(call: ApplicationCall, videoService: VideoService) {
    try {
        val event = call.receive<S3EventNotification>()
        val record = event.Records.firstOrNull() ?: throw BadRequestException("No records in S3 event")
        if (!record.eventName.startsWith("ObjectCreated:")) {
            call.respond(HttpStatusCode.BadRequest)
            return
        }
        val key = record.s3.`object`.key
        val bucket = record.s3.bucket.name
        val video = videoService.findByS3Key(key) ?: throw NotFoundException("Video not found for key: $key")
        videoService.handleS3UploadEvent(video.id, key, bucket)
        call.respond(HttpStatusCode.OK)
    } catch (e: Exception) {
        call.application.environment.log.error("Error processing S3 event", e)
        when (e) {
            is BadRequestException -> call.respond(HttpStatusCode.BadRequest, e.message ?: "Bad Request")
            is NotFoundException -> call.respond(HttpStatusCode.NotFound, e.message ?: "Not Found")
            else -> call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
