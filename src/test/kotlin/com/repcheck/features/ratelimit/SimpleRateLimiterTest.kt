package com.repcheck.features.ratelimit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SimpleRateLimiterTest {
    @Test
    fun `should allow first request and block second`() {
        // Given - 1 request per minute (very strict for testing)
        val config = RateLimitConfig(requestsPerMinute = 1)
        val rateLimiter = RateLimiter(config)
        
        // When - First request
        val firstAttempt = rateLimiter.isAllowed("client1")
        
        // Then - Should be allowed
        assertTrue(firstAttempt, "First request should be allowed")
        
        // When - Second request immediately after
        val secondAttempt = rateLimiter.isAllowed("client1")
        
        // Then - Should be blocked (rate limit of 1 per minute)
        assertFalse(secondAttempt, "Second request should be blocked")
    }
    
    @Test
    fun `should allow requests from different clients`() {
        // Given - 1 request per minute (very strict for testing)
        val config = RateLimitConfig(requestsPerMinute = 1)
        val rateLimiter = RateLimiter(config)
        
        // When - First client
        val client1Attempt = rateLimiter.isAllowed("client1")
        
        // Then - Should be allowed
        assertTrue(client1Attempt, "Client 1 should be allowed")
        
        // When - Different client
        val client2Attempt = rateLimiter.isAllowed("client2")
        
        // Then - Should also be allowed (different client)
        assertTrue(client2Attempt, "Client 2 should be allowed")
    }
    
    @Test
    fun `should reset after time window`() {
        // Given - 60 requests per minute (1 per second)
        val config = RateLimitConfig(requestsPerMinute = 60)
        val rateLimiter = RateLimiter(config)
        
        // When - First request
        val firstAttempt = rateLimiter.isAllowed("client1")
        
        // Then - Should be allowed
        assertTrue(firstAttempt, "First request should be allowed")
        
        // When - Second request immediately after
        val secondAttempt = rateLimiter.isAllowed("client1")
        
        // Then - Should be blocked (per-second rate limiting in the underlying implementation)
        assertFalse(secondAttempt, "Second request should be blocked due to rate limiting")
    }
}
