package com.repcheck.db

import com.repcheck.config.DatabaseConfiguration
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object Database {
    private val log = LoggerFactory.getLogger(Database::class.java)

    @Volatile
    lateinit var dataSource: DataSource
        private set

    fun initialize(configuration: DatabaseConfiguration) {
        val hc = HikariConfig().apply {
            jdbcUrl = configuration.url
            username = configuration.user
            password = configuration.password
            maximumPoolSize = configuration.maxPoolSize
            connectionTimeout = configuration.connectionTimeoutMs
            maxLifetime = configuration.maxLifetimeMs
            idleTimeout = configuration.idleTimeoutMs
            isAutoCommit = true
            poolName = "repcheck-hikari"
        }
        dataSource = HikariDataSource(hc)
        log.info(
            "HikariCP initialized url={}, maxPoolSize={}, connTimeoutMs={}",
            sanitizeUrl(configuration.url), configuration.maxPoolSize, configuration.connectionTimeoutMs
        )
    }

    /** Quick connectivity check used by readiness probes. */
    fun ping(): Boolean = try {
        dataSource.connection.use { conn ->
            conn.createStatement().use { st -> st.execute("SELECT 1") }
        }
        true
    } catch (e: Exception) {
        log.warn("Database ping failed", e)
        false
    }

    private fun sanitizeUrl(url: String): String =
        url.replace(Regex("://([^:@/]+):([^@/]+)@"), "://****:****@")
}
