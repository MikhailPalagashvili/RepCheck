package com.repcheck.config

data class DatabaseConfiguration(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10,
    val connectionTimeoutMs: Long = 2_000,
    val maxLifetimeMs: Long = 30 * 60 * 1000, // 30m
    val idleTimeoutMs: Long = 10 * 60 * 1000  // 10m
)

object AppConfiguration {
    fun database(): DatabaseConfiguration {
        val config = com.typesafe.config.ConfigFactory.load()

        val url = config.getString("db.url")
        val user = config.getString("db.user")
        val password = config.getString("db.password")

        val maxPool = config.getIntOrNull("db.pool.maxPoolSize") ?: 10
        val connTimeout = config.getLongOrNull("db.pool.connectionTimeoutMs") ?: 2_000L
        val maxLifetime = config.getLongOrNull("db.pool.maxLifetimeMs") ?: (30 * 60 * 1000L)
        val idleTimeout = config.getLongOrNull("db.pool.idleTimeoutMs") ?: (10 * 60 * 1000L)

        return DatabaseConfiguration(
            url = url,
            user = user,
            password = password,
            maxPoolSize = maxPool,
            connectionTimeoutMs = connTimeout,
            maxLifetimeMs = maxLifetime,
            idleTimeoutMs = idleTimeout
        )
    }
}

// Helpers to read optional values from Typesafe Config without throwing
private fun com.typesafe.config.Config.getIntOrNull(path: String): Int? =
    if (this.hasPath(path)) this.getInt(path) else null

private fun com.typesafe.config.Config.getLongOrNull(path: String): Long? =
    if (this.hasPath(path)) this.getLong(path) else null
