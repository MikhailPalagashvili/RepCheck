package com.repcheck.features.video.presentation

import com.repcheck.features.video.domain.service.VideoService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.videoRoutes(videoService: VideoService) {
    route("/videos") {

        /**
         * Generate a presigned URL for direct video upload
         */
        post("/presign") {
            // Get authenticated user ID from JWT
            val userId = call.principal<JWTPrincipal>()!!
                .payload.getClaim("userId").asLong()

            // Get optional workoutSetId from query parameters
            val workoutSetId = call.parameters["workoutSetId"]?.toLongOrNull()
                ?: error("Missing workoutSetId")

            // Optional file extension, default to "mp4"
            val fileExtension = call.parameters["fileExtension"] ?: "mp4"

            // Create video record and get presigned URL
            val (video, uploadUrl) = videoService.createVideoAndGetUploadUrl(
                userId = userId,
                workoutSetId = workoutSetId,
                fileExtension = fileExtension
            )

            // Respond with video info + presigned URL
            call.respond(
                mapOf(
                    "videoId" to video.id,
                    "fileExtension" to fileExtension,
                    "uploadUrl" to uploadUrl.toString()
                )
            )
        }

        // Add more video-related routes here (delete, list, mark processed, etc.)
    }
}
