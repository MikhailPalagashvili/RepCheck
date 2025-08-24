package com.repcheck.features.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for coroutine lifecycle and cancellation behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineLifecycleTest {

    private lateinit var testScheduler: TestCoroutineScheduler
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setup() {
        testScheduler = TestCoroutineScheduler()
        testDispatcher = StandardTestDispatcher(testScheduler)
    }

    @Test
    fun `multiple cancellations are safe`() = runTest(testDispatcher) {
        // Create a job and cancel it multiple times
        val job = Job()

        // First cancellation should work
        job.cancel()
        assertTrue(job.isCancelled, "Job should be cancelled after first cancellation")

        // Subsequent cancellations should be no-ops
        job.cancel()
        job.cancel()

        // Should still be cancelled
        assertTrue(job.isCancelled, "Job should remain cancelled after multiple cancellations")
    }

    @Test
    fun `launching in cancelled scope creates cancelled job`() = runTest(testDispatcher) {
        // Create and immediately cancel a scope
        val scope = CoroutineScope(testDispatcher + Job().apply { cancel() })

        // Track if coroutine body was executed
        var coroutineExecuted = false

        // Launch a coroutine in the cancelled scope
        val job = scope.launch {
            coroutineExecuted = true
        }

        // The job should be cancelled immediately
        assertTrue(job.isCancelled, "Job should be cancelled when launched in cancelled scope")

        // The coroutine body should not execute
        assertFalse(coroutineExecuted, "Coroutine body should not execute in cancelled scope")
    }
}
