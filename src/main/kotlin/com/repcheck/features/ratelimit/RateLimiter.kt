package com.repcheck.features.ratelimit

import com.google.common.util.concurrent.RateLimiter.create
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import java.util.concurrent.ConcurrentHashMap
import com.google.common.util.concurrent.RateLimiter as GuavaRateLimiter

data class RateLimitConfig(
    val requestsPerMinute: Int = 60,
    val message: String = "Rate limit exceeded. Please try again later."
)

class RateLimiter(private val config: RateLimitConfig) {
    private val limiters = ConcurrentHashMap<String, GuavaRateLimiter>()

    private fun getOrCreateLimiter(clientId: String) =
        limiters.computeIfAbsent(clientId) { create(config.requestsPerMinute.toDouble() / 60) }

    fun isAllowed(clientId: String) = getOrCreateLimiter(clientId).tryAcquire()
}

class RateLimit(private val config: RateLimitConfig = RateLimitConfig()) {
    private val rateLimiter = RateLimiter(config)

    suspend fun intercept(
        pipeline: PipelineContext<Unit, ApplicationCall>,
        block: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit
    ) {
        val clientId = pipeline.call.request.origin.remoteHost
        if (rateLimiter.isAllowed(clientId)) {
            block(pipeline)
        } else {
            pipeline.call.respond(
                status = HttpStatusCode.TooManyRequests,
                message = mapOf("error" to config.message)
            )
        }
    }
}