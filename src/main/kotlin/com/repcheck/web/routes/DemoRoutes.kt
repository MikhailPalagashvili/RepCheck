package com.repcheck.web.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

fun Route.demoRoutes() {
    // /block?ms=200 -> blocks the event-loop thread (example of what NOT to do)
    get("/block") {
        val ms = call.request.queryParameters["ms"]?.toLongOrNull() ?: 200L
        Thread.sleep(ms)
        call.respondText("blocked for ${ms}ms")
    }

    // /nonblock?ms=200 -> suspends without blocking the event-loop thread
    get("/nonblock") {
        val ms = call.request.queryParameters["ms"]?.toLongOrNull() ?: 200L
        delay(ms)
        call.respondText("non-blocking delay for ${ms}ms")
    }

    // /io-offload?ms=200 -> simulates blocking I/O but offloads to IO dispatcher
    get("/io-offload") {
        val ms = call.request.queryParameters["ms"]?.toLongOrNull() ?: 200L
        withContext(Dispatchers.IO) {
            Thread.sleep(ms)
        }
        call.respondText("blocking I/O offloaded for ${ms}ms")
    }
}
