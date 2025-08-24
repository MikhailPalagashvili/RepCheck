package com.repcheck.features.ratelimit

import io.ktor.server.application.*
import io.ktor.server.routing.*

/**
 * Apply rate limiting to a route with the specified configuration
 *
 * @param requestsPerMinute Maximum number of requests allowed per minute
 * @param message Error message to return when rate limit is exceeded
 * @param block The route block to apply rate limiting to
 */
fun Route.rateLimited(
    requestsPerMinute: Int = 60,
    message: String = "Rate limit exceeded. Please try again later.",
    block: Route.() -> Unit
) {
    val rateLimit = RateLimit(RateLimitConfig(requestsPerMinute, message))

    route("") {
        intercept(ApplicationCallPipeline.Call) {
            rateLimit.intercept(this) {}
        }

        block()
    }
}

/**
 * Install rate limiting for all routes in the application
 *
 * @param requestsPerMinute Maximum number of requests allowed per minute
 * @param message Error message to return when rate limit is exceeded
 */
fun Application.installRateLimiting(
    requestsPerMinute: Int = 60,
    message: String = "Rate limit exceeded. Please try again later."
) {
    val rateLimit = RateLimit(RateLimitConfig(requestsPerMinute, message))

    routing {
        intercept(ApplicationCallPipeline.Call) {
            rateLimit.intercept(this) {}
        }
    }
}
