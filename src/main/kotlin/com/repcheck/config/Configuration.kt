package com.repcheck.config

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10,
    val connectionTimeoutMs: Long = 2_000,
    val maxLifetimeMs: Long = 30 * 60 * 1000, // 30m
    val idleTimeoutMs: Long = 10 * 60 * 1000  // 10m
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expiresSeconds: Long = 900
)

object AppConfig {
    fun databaseConfig(): DatabaseConfig {
        val config = com.typesafe.config.ConfigFactory.load()

        val url = config.getString("db.url")
        val user = config.getString("db.user")
        val password = config.getString("db.password")

        val maxPool = config.getIntOrNull("db.pool.maxPoolSize") ?: 10
        val connTimeout = config.getLongOrNull("db.pool.connectionTimeoutMs") ?: 2_000L
        val maxLifetime = config.getLongOrNull("db.pool.maxLifetimeMs") ?: (30 * 60 * 1000L)
        val idleTimeout = config.getLongOrNull("db.pool.idleTimeoutMs") ?: (10 * 60 * 1000L)

        return DatabaseConfig(
            url = url,
            user = user,
            password = password,
            maxPoolSize = maxPool,
            connectionTimeoutMs = connTimeout,
            maxLifetimeMs = maxLifetime,
            idleTimeoutMs = idleTimeout
        )
    }

    fun jwtConfig(): JwtConfig {
        val config = com.typesafe.config.ConfigFactory.load()
        val secret = config.getString("jwt.secret")
        val issuer = config.getString("jwt.issuer")
        val audience = config.getString("jwt.audience")
        val exp = config.getLongOrNull("jwt.expiresSeconds") ?: 900L
        return JwtConfig(secret = secret, issuer = issuer, audience = audience, expiresSeconds = exp)
    }
}

private fun com.typesafe.config.Config.getIntOrNull(path: String): Int? =
    if (this.hasPath(path)) this.getInt(path) else null

private fun com.typesafe.config.Config.getLongOrNull(path: String): Long? =
    if (this.hasPath(path)) this.getLong(path) else null