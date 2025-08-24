package com.repcheck.db

import com.repcheck.config.DatabaseConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object Database {
    private val log = LoggerFactory.getLogger(Database::class.java)

    /**
     * Initialize the database connection and run migrations
     */
    fun init(config: DatabaseConfig) {
        // Initialize the database connection pool and Exposed
        DatabaseFactory.init(config)
        // Test the connection
        if (ping()) {
            log.info("Database connection established successfully")
        } else {
            log.warn("Database connection test failed")
        }
    }

    /**
     * Quick connectivity check used by readiness probes
     */
    fun ping(): Boolean = try {
        transaction {
            // Execute a simple query to test the connection
            exec("SELECT 1") {}
            true
        }
    } catch (e: Exception) {
        log.warn("Database ping failed", e)
        false
    }
}
