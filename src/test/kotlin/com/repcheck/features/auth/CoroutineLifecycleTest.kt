package com.repcheck.features.auth

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

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
    fun `cancelling parent job cancels all children`() = runTest {
        // Create a parent job and scope with test dispatcher
        val parentJob = Job()
        val scope = CoroutineScope(testDispatcher + parentJob)
        
        // Track if child coroutine was cancelled
        var childCancelled = false
        val childStarted = CompletableDeferred<Unit>()
        
        // Launch a child coroutine
        val childJob = scope.launch {
            childStarted.complete(Unit) // Signal that coroutine has started
            try {
                while (isActive) {
                    delay(1000) // Will be cancelled
                }
                fail("Should not reach here")
            } catch (e: CancellationException) {
                childCancelled = true
                throw e
            }
        }
        
        // Wait for child to start and process any pending coroutines
        runBlocking { childStarted.await() }
        testScheduler.advanceUntilIdle()
        
        // When: Cancel the parent job
        scope.cancel()
        
        // Process any pending coroutines after cancellation
        testScheduler.advanceUntilIdle()
        
        // Then:
        assertTrue(childJob.isCancelled, "Child job should be marked as cancelled")
        assertTrue(childJob.isCompleted, "Child job should be completed")
        assertTrue(childCancelled, "Child coroutine should be cancelled")
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
