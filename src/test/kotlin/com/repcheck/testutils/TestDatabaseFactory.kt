package com.repcheck.testutils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

/**
 * Sets up and tears down an in-memory H2 test database.
 */
object TestDatabaseFactory {
    private var dataSource: HikariDataSource? = null

    fun init() {
        if (dataSource == null) {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
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

    fun close() {
        dataSource?.close()
        dataSource = null
    }
}
