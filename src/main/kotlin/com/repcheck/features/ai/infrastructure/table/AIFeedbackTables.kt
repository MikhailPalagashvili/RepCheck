package com.repcheck.features.ai.infrastructure.table

import com.repcheck.features.ai.domain.model.AnalysisStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb
import java.time.Instant

object AIFeedbackTable : Table("ai_feedback") {
    val id = long("id").autoIncrement()
    val videoId = long("video_id").references(
        com.repcheck.features.video.infrastructure.table.WorkoutVideos.id,
        onDelete = ReferenceOption.CASCADE
    )
    val workoutSetId = long("workout_set_id").references(
        com.repcheck.features.workout.infrastructure.table.WorkoutSets.id,
        onDelete = ReferenceOption.CASCADE
    )
    val analysisResults = jsonb<JsonObject>(
        name = "analysis_results",
        serialize = { it.toString() },
        deserialize = { raw ->
            val s = raw as? String
            s?.let { Json.parseToJsonElement(it).jsonObject } ?: JsonObject(emptyMap())
        }
    )
    val status = enumerationByName<AnalysisStatus>("status", 20).default(AnalysisStatus.PENDING)
    val error = text("error").nullable()
    val processedAt = timestamp("processed_at").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id, name = "pk_ai_feedback")
}
