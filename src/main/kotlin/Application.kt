package com.repcheck

import com.repcheck.config.AppConfig
import com.repcheck.db.Database
import com.repcheck.web.plugins.installMonitoring
import com.repcheck.web.plugins.installSerialization
import com.repcheck.web.routes.healthRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway.configure

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    Database.initialize(AppConfig.databaseConfig())
    runFlywayMigrations()
    installMonitoring()
    installSerialization()
    routing {
        healthRoutes()
    }
}

private fun runFlywayMigrations() = configure().dataSource(Database.dataSource).load().migrate()
