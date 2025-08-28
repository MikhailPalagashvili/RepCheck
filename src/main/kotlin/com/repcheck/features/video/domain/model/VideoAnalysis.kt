package com.repcheck.features.video.domain.model

import kotlinx.serialization.json.JsonObject

data class VideoAnalysis(
    val id: Long = 0,
    val videoId: Long,
    val analysisData: JsonObject, // Or a more specific domain model
    val confidence: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)