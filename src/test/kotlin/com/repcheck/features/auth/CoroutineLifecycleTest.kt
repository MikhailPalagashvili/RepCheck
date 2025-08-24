package com.repcheck.features.auth

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Tests for coroutine lifecycle and cancellation behavior.
 */
class CoroutineLifecycleTest {
    
    @Test
    fun `cancelling parent job cancels all children`() = runTest {
        // Create a parent job and scope
        val parentJob = Job()
        val scope = CoroutineScope(Dispatchers.Default + parentJob)
        
        // Track if child coroutine was cancelled
        var childCancelled = false
        
        // Launch a child coroutine
        val childJob = scope.launch {
            try {
                delay(1000) // Will be cancelled
            } catch (e: CancellationException) {
                childCancelled = true
                throw e
            }
        }
        
        // When: Cancel the parent job
        parentJob.cancel("Test cancellation")
        
        // Wait for the child to process cancellation
        childJob.join()
        
        // Then:
        assertTrue(childCancelled, "Child coroutine should be cancelled")
        assertTrue(childJob.isCancelled, "Child job should be marked as cancelled")
        assertTrue(childJob.isCompleted, "Child job should be completed")
    }
    
    @Test
    fun `multiple cancellations are safe`() = runTest {
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
    fun `launching in cancelled scope creates cancelled job`() = runTest {
        // Create and immediately cancel a scope
        val scope = CoroutineScope(Dispatchers.Default + Job().apply { cancel() })
        
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
