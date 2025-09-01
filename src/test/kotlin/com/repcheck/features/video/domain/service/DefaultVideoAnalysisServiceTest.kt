package com.repcheck.features.video.domain.service

import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.model.VideoAnalysis
import com.repcheck.features.video.domain.model.WorkoutVideo
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class DefaultVideoAnalysisServiceTest : BehaviorSpec({
    val service = DefaultVideoAnalysisService()

    given("extractFeatures") {
        `when`("called with a video") {
            val video = WorkoutVideo(
                id = 1L,
                userId = 1L,
                workoutSetId = 1L,
                s3Key = "test/video.mp4",
                s3Bucket = "test-bucket"
            )
            then("should return VideoFeatures with default values") {
                runTest {
                    val features = service.extractFeatures(video)
                    features.duration shouldBe 0L
                    features.keyFrames shouldBe emptyList()
                    features.motionVectors shouldBe emptyList()
                    features.audioFeatures shouldBe null
                }
            }
        }
    }

    given("analyzeWorkout") {
        `when`("called with video features") {
            val features = VideoFeatures(
                duration = 60000, // 1 minute
                keyFrames = emptyList(),
                motionVectors = emptyList()
            )
            then("should return WorkoutAnalysis with default values") {
                runTest {
                    val analysis = service.analyzeWorkout(features)
                    analysis.exerciseType shouldBe ExerciseType.SQUAT
                    analysis.repetitions shouldBe 0
                    analysis.formScore shouldBe 0f
                    analysis.feedback shouldBe emptyList()
                    analysis.metrics shouldBe emptyMap()
                }
            }
        }
    }

    given("generateReport") {
        `when`("called with workout analysis") {
            val analysis = WorkoutAnalysis(
                exerciseType = ExerciseType.SQUAT,
                repetitions = 10,
                formScore = 0.85f,
                feedback = listOf("Good form!", "Keep your back straight"),
                metrics = mapOf("range_of_motion" to 0.8f, "speed" to 1.2f)
            )
            then("should generate a valid VideoAnalysis report") {
                runTest {
                    val report = service.generateReport(analysis)

                    report.videoId shouldBe 1L
                    report.confidence shouldBe 0.85f

                    // Verify JSON structure
                    val data = report.analysisData
                    data["exerciseType"] shouldBe JsonPrimitive("SQUAT")
                    data["repetitions"] shouldBe JsonPrimitive(10)
                    data["formScore"] shouldBe JsonPrimitive(0.85f)

                    val feedback = data["feedback"]?.jsonArray
                    feedback?.size shouldBe 2

                    val metrics = data["metrics"]?.jsonObject
                    metrics?.size shouldBe 2
                }
            }
        }
    }

    given("error handling") {
        `when`("an error occurs during analysis") {
            val failingService = object : VideoAnalysisService {
                override suspend fun extractFeatures(video: WorkoutVideo): VideoFeatures {
                    throw RuntimeException("Failed to extract features")
                }

                override suspend fun analyzeWorkout(features: VideoFeatures): WorkoutAnalysis {
                    throw UnsupportedOperationException()
                }

                override suspend fun generateReport(analysis: WorkoutAnalysis): VideoAnalysis {
                    throw UnsupportedOperationException()
                }
            }

            then("should propagate the exception") {
                runTest {
                    val result = runCatching {
                        failingService.extractFeatures(
                            WorkoutVideo(
                                id = 1L,
                                userId = 1L,
                                workoutSetId = 1L,
                                s3Key = "test/video.mp4",
                                s3Bucket = "test-bucket"
                            )
                        )
                    }

                    result.isFailure shouldBe true
                    result.exceptionOrNull()?.message shouldBe "Failed to extract features"
                }
            }
        }
    }
})
