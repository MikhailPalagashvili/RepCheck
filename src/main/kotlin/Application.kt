package com.repcheck

import com.repcheck.plugins.configurePlugins
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Application")

fun main() {
    // Load application.conf by default
    System.setProperty("config.file", "src/main/resources/application.conf")
    
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    try {
        configurePlugins()
        log.info("Application started successfully")
    } catch (e: Exception) {
        log.error("Application failed to start: ${e.message}", e)
        throw e
    }
}
