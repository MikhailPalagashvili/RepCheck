package com.repcheck.features.ai.infrastructure.table

import com.repcheck.features.video.infrastructure.table.WorkoutVideos
import com.repcheck.features.workout.infrastructure.table.WorkoutSets
import com.repcheck.features.ai.domain.model.AnalysisStatus
import com.repcheck.features.ai.domain.model.AnalysisResults
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb
import java.time.Instant

object AIFeedbackTable : Table("ai_feedback") {
    val id = long("id").autoIncrement()
    val videoId = long("video_id").references(WorkoutVideos.id, onDelete = ReferenceOption.CASCADE)
    val workoutSetId = long("workout_set_id").references(WorkoutSets.id, onDelete = ReferenceOption.CASCADE)
    val analysisResults = jsonb<AnalysisResults>(
        name = "analysis_results",
        serialize = { value -> Json.encodeToString(AnalysisResults.serializer(), value) },
        deserialize = { raw ->
            try {
                Json.decodeFromString(AnalysisResults.serializer(), raw)
            } catch (e: Exception) {
                AnalysisResults()
            }
        }
    )
    val status = enumerationByName<AnalysisStatus>("status", 20).default(AnalysisStatus.PENDING)
    val error = text("error").nullable()
    val processedAt = timestamp("processed_at").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id, name = "pk_ai_feedback")
}
