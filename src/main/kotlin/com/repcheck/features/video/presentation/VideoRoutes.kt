package com.repcheck.features.video.presentation

import com.repcheck.infrastructure.s3.S3UploadService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.videoRoutes(s3UploadService: S3UploadService) {
    route("/videos") {
        post("/presign") {
            val videoId = call.parameters["videoId"] ?: UUID.randomUUID().toString()
            val contentType = call.parameters["contentType"] ?: "video/mp4"
            
            val uploadUrl = s3UploadService.generateVideoUploadUrl(
                videoId = videoId,
                fileExtension = contentType.split('/').last(),
                contentType = contentType
            )
            
            call.respond(
                mapOf(
                    "uploadUrl" to uploadUrl.toString(),
                    "videoId" to videoId,
                    "contentType" to contentType
                )
            )
        }
    }
}
