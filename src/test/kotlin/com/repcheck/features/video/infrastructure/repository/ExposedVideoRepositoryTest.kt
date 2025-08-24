package com.repcheck.features.video.infrastructure.repository

import com.repcheck.features.video.domain.model.VideoStatus
import com.repcheck.features.video.infrastructure.table.WorkoutVideos
import com.repcheck.features.workout.domain.model.LiftType
import com.repcheck.features.workout.infrastructure.table.Lifts
import com.repcheck.features.workout.infrastructure.table.WorkoutSets
import com.repcheck.features.workout.infrastructure.table.Workouts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.time.Instant
import kotlin.test.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedVideoRepositoryTest {
    private lateinit var repository: ExposedVideoRepository
    private lateinit var database: Database

    private fun createTestWorkoutAndSet(userId: Long = 1L): Long {
        return transaction(database) {
            // First, ensure we have a lift
            val liftId = Lifts.insertAndGetId { lift ->
                lift[Lifts.type] = LiftType.BENCH_PRESS
                lift[Lifts.name] = "Test Lift"
                lift[Lifts.createdAt] = Instant.now()
                lift[Lifts.updatedAt] = Instant.now()
            }.value

            // Create a workout
            val workoutId = Workouts.insertAndGetId { workout ->
                workout[Workouts.userId] = userId
                workout[Workouts.startTime] = Instant.now()
                workout[Workouts.createdAt] = Instant.now()
                workout[Workouts.updatedAt] = Instant.now()
            }.value

            // Create a workout set
            WorkoutSets.insertAndGetId { workoutSet ->
                workoutSet[WorkoutSets.workoutId] = workoutId
                workoutSet[WorkoutSets.liftId] = liftId
                workoutSet[WorkoutSets.weight] = 100.0
                workoutSet[WorkoutSets.reps] = 10
                workoutSet[WorkoutSets.completedAt] = Instant.now()
                workoutSet[WorkoutSets.createdAt] = Instant.now()
                workoutSet[WorkoutSets.updatedAt] = Instant.now()
            }.value
        }
    }

    @BeforeEach
    fun setup() {
        // Initialize in-memory H2 database
        database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", driver = "org.h2.Driver")
        
        transaction(database) {
            // Create tables in the correct order to respect foreign key constraints
            SchemaUtils.create(Lifts, Workouts, WorkoutSets, WorkoutVideos)
        }
        
        repository = ExposedVideoRepository()
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            // Drop all tables
            SchemaUtils.drop(WorkoutVideos, WorkoutSets, Workouts)
        }
    }

    @Test
    fun `create should insert and return video`() {
        val workoutSetId = createTestWorkoutAndSet()

        val video = repository.create(
            userId = 1L,
            workoutSetId = workoutSetId,
            s3Key = "test-key",
            s3Bucket = "test-bucket",
            status = VideoStatus.UPLOADING
        )

        assertNotNull(video.id)
        assertEquals(1L, video.userId)
        assertEquals(1L, video.workoutSetId)
        assertEquals("test-key", video.s3Key)
        assertEquals("test-bucket", video.s3Bucket)
        assertEquals(VideoStatus.UPLOADING, video.status)
    }

    @Test
    fun `findById should return null for non-existent video`() {
        val result = repository.findById(999L)
        assertNull(result)
    }

    @Test
    fun `findById should return video when it exists`() {
        val workoutSetId = createTestWorkoutAndSet()

        val created = repository.create(
            userId = 1L,
            workoutSetId = workoutSetId,
            s3Key = "test-key",
            s3Bucket = "test-bucket",
            status = VideoStatus.UPLOADING
        )

        val found = repository.findById(created.id)
        assertNotNull(found)
        assertEquals(created.id, found.id)
    }

    @Test
    fun `updateStatus should update video status`() {
        val workoutSetId = createTestWorkoutAndSet()

        val created = repository.create(
            userId = 1L,
            workoutSetId = workoutSetId,
            s3Key = "test-key",
            s3Bucket = "test-bucket",
            status = VideoStatus.UPLOADING
        )

        val updated = repository.updateStatus(created.id, VideoStatus.PROCESSED)
        assertTrue(updated)

        val found = repository.findById(created.id)
        assertEquals(VideoStatus.PROCESSED, found?.status)
    }

    @Test
    fun `updateDuration should update video duration`() {
        val workoutSetId = createTestWorkoutAndSet()

        val created = repository.create(
            userId = 1L,
            workoutSetId = workoutSetId,
            s3Key = "test-key",
            s3Bucket = "test-bucket",
            status = VideoStatus.PROCESSED
        )

        val updated = repository.updateDuration(created.id, 120)
        assertTrue(updated)

        val found = repository.findById(created.id)
        assertEquals(120, found?.durationSeconds)
    }

    @Test
    fun `updateFileSize should update file size`() {
        val workoutSetId = createTestWorkoutAndSet()

        val created = repository.create(
            userId = 1L,
            workoutSetId = workoutSetId,
            s3Key = "test-key",
            s3Bucket = "test-bucket",
            status = VideoStatus.PROCESSED
        )

        val fileSize = 1024L * 1024 // 1MB
        val updated = repository.updateFileSize(created.id, fileSize)
        assertTrue(updated)

        val found = repository.findById(created.id)
        assertEquals(fileSize, found?.fileSizeBytes)
    }

    @Test
    fun `attachToSet should update workout set ID`() {
        val initialSetId = createTestWorkoutAndSet()
        val newSetId = createTestWorkoutAndSet()

        val created = repository.create(
            userId = 1L,
            workoutSetId = initialSetId,
            s3Key = "test-key",
            s3Bucket = "test-bucket",
            status = VideoStatus.PROCESSED
        )

        val updated = repository.attachToSet(created.id, newSetId)
        assertTrue(updated)

        val found = repository.findById(created.id)
        assertEquals(newSetId, found?.workoutSetId)
    }

    @Test
    fun `delete should remove video`() {
        val workoutSetId = createTestWorkoutAndSet()

        val created = repository.create(
            userId = 1L,
            workoutSetId = workoutSetId,
            s3Key = "test-key",
            s3Bucket = "test-bucket",
            status = VideoStatus.PROCESSED
        )

        val deleted = repository.delete(created.id)
        assertTrue(deleted)

        val found = repository.findById(created.id)
        assertNull(found)
    }

    @Test
    fun `findBySet should return videos for set`() {
        val workoutSetId = createTestWorkoutAndSet()

        repository.create(
            userId = 1L,
            workoutSetId = workoutSetId,
            s3Key = "test-key-1",
            s3Bucket = "test-bucket",
            status = VideoStatus.PROCESSED
        )
        repository.create(
            userId = 1L,
            workoutSetId = workoutSetId,
            s3Key = "test-key-2",
            s3Bucket = "test-bucket",
            status = VideoStatus.PROCESSED
        )

        val videos = repository.findBySet(workoutSetId)
        assertEquals(2, videos.size)
        assertTrue(videos.all { it.workoutSetId == workoutSetId })
    }

    @Test
    fun `findByUser should return paginated videos for user`() {
        val userId = 1L
        // Create 3 videos with their own workout sets
        repeat(3) { i ->
            val workoutSetId = createTestWorkoutAndSet(userId)
            repository.create(
                userId = userId,
                workoutSetId = workoutSetId,
                s3Key = "test-key-$i",
                s3Bucket = "test-bucket",
                status = VideoStatus.PROCESSED
            )
        }

        // Get first page (2 items)
        val firstPage = repository.findByUser(userId, limit = 2, offset = 0)
        assertEquals(2, firstPage.size)

        // Get second page (1 item)
        val secondPage = repository.findByUser(userId, limit = 2, offset = 2)
        assertEquals(1, secondPage.size)
    }

    @Test
    fun `updateStatus should return false for non-existent video`() {
        val updated = repository.updateStatus(999L, VideoStatus.PROCESSED)
        assertFalse(updated)
    }
}
