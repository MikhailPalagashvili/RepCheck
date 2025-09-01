package com.repcheck.features.video.domain.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class ProcessingProgressTrackerTest : BehaviorSpec({
    val tracker = InMemoryProgressTracker()

    given("InMemoryProgressTracker") {
        `when`("updating progress") {
            then("should store and retrieve progress correctly") {
                runTest {
                    // Test initial state
                    tracker.getProgress(1L) shouldBe null

                    // Test updating progress
                    tracker.updateProgress(1L, 50)
                    tracker.getProgress(1L) shouldBe 50

                    // Test updating progress again
                    tracker.updateProgress(1L, 75)
                    tracker.getProgress(1L) shouldBe 75
                }
            }

            then("should clamp progress between 0 and 100") {
                runTest {
                    // Test below minimum
                    tracker.updateProgress(2L, -10)
                    tracker.getProgress(2L) shouldBe 0

                    // Test above maximum
                    tracker.updateProgress(3L, 150)
                    tracker.getProgress(3L) shouldBe 100
                }
            }
        }

        `when`("removing progress") {
            then("should remove the progress entry") {
                runTest {
                    // Setup
                    tracker.updateProgress(4L, 50)
                    tracker.getProgress(4L) shouldBe 50

                    // Test removal
                    tracker.removeProgress(4L)
                    tracker.getProgress(4L) shouldBe null
                }
            }

            then("should handle non-existent video IDs gracefully") {
                runTest {
                    // Should not throw
                    tracker.removeProgress(999L)
                }
            }
        }

        `when`("handling concurrent access") {
            then("should handle concurrent updates safely") {
                runTest {
                    val iterations = 1000
                    val videoId = 5L

                    // Launch multiple coroutines to update progress
                    (1..iterations).map { i ->
                        launch {
                            tracker.updateProgress(videoId, i % 101)
                        }
                    }.forEach { it.join() }

                    // Final value should be within valid range (0-100)
                    val finalProgress = tracker.getProgress(videoId)
                    finalProgress.shouldNotBeNull().shouldBeIn(0..100)

                    // Verify the value was actually set by one of the updates
                    finalProgress.shouldNotBeNull().shouldBeIn(0..100)
                }
            }
        }
    }
})
