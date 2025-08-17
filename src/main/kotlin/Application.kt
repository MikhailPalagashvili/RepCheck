package com.repcheck

import com.repcheck.config.AppConfiguration
import com.repcheck.db.Database
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway.configure
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger(Application::class.java)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    Database.initialize(AppConfiguration.database())
    runFlywayMigrations()
    installPlugins()
    routing {
        get("/health/live") { call.respondText("OK") }
        get("/health/ready") {
            val ok = try {
                withContext(Dispatchers.IO) {
                    Database.dataSource.connection.use { conn ->
                        conn.createStatement().use { st ->
                            st.execute("SELECT 1")
                        }
                    }
                }
                true
            } catch (e: Exception) {
                logger.warn("Readiness DB check failed", e)
                false
            }
            if (ok) call.respondText("OK") else call.respond(HttpStatusCode.ServiceUnavailable, "DB not ready")
        }

        test()
    }
}

private fun runFlywayMigrations() = configure().dataSource(Database.dataSource).load().migrate()

private fun Application.installPlugins() {
    install(CallId) {
        header("X-Request-Id")
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() && it.length <= 128 }
    }
    install(CallLogging) {
        mdc("requestId") { call -> call.callId }
        format { call ->
            val status = call.response.status()?.value
            val method = call.request.httpMethod.value
            val uri = call.request.path()
            "${method} ${uri} -> ${status}"
        }
    }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = false
            ignoreUnknownKeys = true
        })
    }
}

private fun Routing.test() {
    // DEMO: Blocking vs non-blocking
    // /block?ms=200 -> blocks the event-loop thread
    get("/block") {
        val ms = call.request.queryParameters["ms"]?.toLongOrNull() ?: 200L
        // BAD: blocks the event loop
        Thread.sleep(ms)
        call.respondText("blocked for ${ms}ms")
    }

    // /nonblock?ms=200 -> suspends without blocking the event-loop thread
    get("/nonblock") {
        val ms = call.request.queryParameters["ms"]?.toLongOrNull() ?: 200L
        // GOOD: suspends; frees the event-loop thread
        delay(ms)
        call.respondText("non-blocking delay for ${ms}ms")
    }

    // /io-offload?ms=200 -> simulates blocking I/O but offloads to IO dispatcher
    get("/io-offload") {
        val ms = call.request.queryParameters["ms"]?.toLongOrNull() ?: 200L
        // Offload blocking work to a worker pool
        withContext(Dispatchers.IO) {
            Thread.sleep(ms)
        }
        call.respondText("blocking I/O offloaded for ${ms}ms")
    }
}
