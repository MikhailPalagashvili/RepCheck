package com.repcheck

import com.repcheck.config.AppConfiguration
import com.repcheck.db.Database
import com.repcheck.web.plugins.installMonitoring
import com.repcheck.web.plugins.installSerialization
import com.repcheck.web.routes.healthRoutes
import com.repcheck.web.routes.demoRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    installMonitoring()
    installSerialization()
    routing {
        // Health endpoints
        healthRoutes()
        // Demo/test endpoints (non-blocking vs blocking)
        demoRoutes()
    }
}

private fun runFlywayMigrations() = configure().dataSource(Database.dataSource).load().migrate()
