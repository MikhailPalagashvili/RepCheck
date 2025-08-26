package com.repcheck.infrastructure.persistence.db

import com.repcheck.infrastructure.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object DatabaseFactory {
    fun init(config: DatabaseConfig) {
        // Create HikariCP connection pool
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            connectionTimeout = config.connectionTimeoutMs
            maxLifetime = config.maxLifetimeMs
            idleTimeout = config.idleTimeoutMs
            isAutoCommit = true
            poolName = "repcheck-hikari"
            driverClassName = config.driver
        }

        // Initialize Exposed with HikariCP
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        // Configure Exposed
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ

        // Configure SQL logger in development
        if (System.getenv("ENV") != "production") {
            transaction {
                addLogger(StdOutSqlLogger)
            }
        }
    }

    // Helper function for transactions
    fun <T> query(block: () -> T): T = transaction {
        addLogger(StdOutSqlLogger)
        block()
    }
}
