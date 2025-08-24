package com.repcheck.features.ai.infrastructure.db.repository

import com.repcheck.features.ai.domain.model.AIFeedback
import com.repcheck.features.ai.domain.model.AnalysisResults
import com.repcheck.features.ai.domain.model.AnalysisStatus
import com.repcheck.features.ai.domain.repository.AIFeedbackRepository
import com.repcheck.features.ai.infrastructure.db.tables.AIFeedbackTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ExposedAIFeedbackRepository : AIFeedbackRepository {
    private fun rowToDomain(row: ResultRow): AIFeedback = AIFeedback(
        id = row[AIFeedbackTable.id],
        videoId = row[AIFeedbackTable.videoId],
        workoutSetId = row[AIFeedbackTable.workoutSetId],
        analysisResults = row[AIFeedbackTable.analysisResults],
        status = row[AIFeedbackTable.status],
        error = row[AIFeedbackTable.error],
        processedAt = row[AIFeedbackTable.processedAt]?.toEpochMilli(),
        createdAt = row[AIFeedbackTable.createdAt].toEpochMilli()
    )

    override fun insert(
        videoId: Long,
        workoutSetId: Long,
        analysisResults: AnalysisResults,
        status: AnalysisStatus,
        error: String?,
        processedAt: Instant?,
    ): AIFeedback = transaction {
        val id = AIFeedbackTable.insert { st ->
            st[AIFeedbackTable.videoId] = videoId
            st[AIFeedbackTable.workoutSetId] = workoutSetId
            st[AIFeedbackTable.analysisResults] = analysisResults
            st[AIFeedbackTable.status] = status
            st[AIFeedbackTable.error] = error
            st[AIFeedbackTable.processedAt] = processedAt
            // createdAt has DB default; omit to let DB set it
        }[AIFeedbackTable.id]

        AIFeedbackTable.select { AIFeedbackTable.id eq id }
            .single()
            .let(::rowToDomain)
    }

    override fun updateStatus(
        id: Long,
        status: AnalysisStatus,
        error: String?,
        processedAt: Instant?,
    ): Boolean = transaction {
        val updated = AIFeedbackTable.update({ AIFeedbackTable.id eq id }) { st ->
            st[AIFeedbackTable.status] = status
            st[AIFeedbackTable.error] = error
            st[AIFeedbackTable.processedAt] = processedAt
        }
        updated > 0
    }

    override fun findById(id: Long): AIFeedback? = transaction {
        AIFeedbackTable.select { AIFeedbackTable.id eq id }
            .limit(1)
            .singleOrNull()
            ?.let(::rowToDomain)
    }

    override fun findByVideoId(videoId: Long): List<AIFeedback> = transaction {
        AIFeedbackTable.select { AIFeedbackTable.videoId eq videoId }
            .orderBy(AIFeedbackTable.createdAt to SortOrder.DESC)
            .map(::rowToDomain)
    }

    override fun findByWorkoutSetId(workoutSetId: Long): List<AIFeedback> = transaction {
        AIFeedbackTable.select { AIFeedbackTable.workoutSetId eq workoutSetId }
            .orderBy(AIFeedbackTable.createdAt to SortOrder.DESC)
            .map(::rowToDomain)
    }

    override fun delete(id: Long): Boolean = transaction {
        AIFeedbackTable.deleteWhere { AIFeedbackTable.id eq id } > 0
    }
}

