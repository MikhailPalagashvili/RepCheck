package com.repcheck.testutils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

/**
 * Manages a test database connection for integration tests.
 */
object TestDatabaseFactory {
    private var dataSource: HikariDataSource? = null

    /**
     * Initializes the test database with an in-memory H2 database
     */
    fun init() {
        if (dataSource == null) {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                maximumPoolSize = 3
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }
            dataSource = HikariDataSource(config)
            Database.connect(dataSource!!)
        }
    }

    /**
     * Closes the database connection and cleans up resources
     */
    fun close() {
        dataSource?.close()
        dataSource = null
    }
}
