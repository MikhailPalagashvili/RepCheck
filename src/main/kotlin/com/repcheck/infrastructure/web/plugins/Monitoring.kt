package com.repcheck.infrastructure.web.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import java.util.*

fun Application.installMonitoring() {
    install(CallId) {
        header("X-Request-Id")
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() && it.length <= 128 }
    }
    install(io.ktor.server.plugins.callloging.CallLogging) {
        mdc("requestId") { call -> call.callId ?: "unknown" }
        format { call ->
            val status = call.response.status()?.value
            val method = call.request.httpMethod.value
            val uri = call.request.uri
            "$method $uri -> $status"
        }
    }
}
