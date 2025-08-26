package com.repcheck.features.video.presentation

import com.repcheck.common.exceptions.BadRequestException
import com.repcheck.common.exceptions.ForbiddenException
import com.repcheck.common.exceptions.NotFoundException
import com.repcheck.common.extensions.getUserId
import com.repcheck.features.video.domain.service.VideoService
import com.repcheck.features.video.presentation.dto.UploadCompleteRequest
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

        post("/complete") {
            call.getUserId() // Just to verify authentication
            val request = call.receive<UploadCompleteRequest>()

            val video = videoService.completeVideoUpload(
                videoId = request.videoId,
                fileSizeBytes = request.fileSizeBytes,
                durationSeconds = request.durationSeconds
            ) ?: throw NotFoundException("Video not found")

            call.respond(HttpStatusCode.OK, video.toResponse())
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
}
