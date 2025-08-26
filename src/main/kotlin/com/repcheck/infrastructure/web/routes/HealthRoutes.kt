package com.repcheck.infrastructure.web.routes

import com.repcheck.infrastructure.persistence.db.Database
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.healthRoutes(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    get("/health/live") { call.respondText("OK") }
    get("/health/ready") {
        val ok = withContext(dispatcher) { Database.ping() }
        if (ok) call.respondText("OK") else call.respond(HttpStatusCode.ServiceUnavailable, "DB not ready")
    }
}
