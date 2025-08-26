package com.repcheck.infrastructure.config

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Transaction

/**
 * Configuration for database connection
 */
data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val driver: String = "org.postgresql.Driver",
    val maxPoolSize: Int = 10,
    val connectionTimeoutMs: Long = 30000,
    val maxLifetimeMs: Long = 600000,
    val idleTimeoutMs: Long = 60000,
    var sqlLogger: (Transaction.(String) -> Unit)? = null
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expiresInSeconds: Long = 86400 // Default: 24 hours
)

/**
 * Application configuration
 */
object AppConfig {
    private val config = HoconApplicationConfig(ConfigFactory.load())

    /**
     * Get database configuration from application.conf
     */
    fun databaseConfig(): DatabaseConfig {
        return DatabaseConfig(
            url = config.property("db.url").getString(),
            user = config.property("db.user").getString(),
            password = config.property("db.password").getString(),
            driver = config.propertyOrNull("db.driver")?.getString() ?: "org.postgresql.Driver",
            maxPoolSize = config.property("db.pool.maxPoolSize").getString().toIntOrNull() ?: 10,
            connectionTimeoutMs = config.property("db.pool.connectionTimeoutMs").getString().toLongOrNull() ?: 30000,
            maxLifetimeMs = config.property("db.pool.maxLifetimeMs").getString().toLongOrNull() ?: 600000,
            idleTimeoutMs = config.property("db.pool.idleTimeoutMs").getString().toLongOrNull() ?: 60000
        )
    }

    /**
     * Get JWT configuration from application.conf
     */
    fun jwtConfig(): JwtConfig {
        return JwtConfig(
            secret = config.property("jwt.secret").getString(),
            issuer = config.property("jwt.issuer").getString(),
            audience = config.property("jwt.audience").getString(),
            realm = config.property("jwt.realm").getString()
        )
    }
}