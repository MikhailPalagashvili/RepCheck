package com.repcheck.features.user.application.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.repcheck.config.AppConfig
import java.util.*

class JwtProvider {
    private val config = AppConfig.jwtConfig()
    private val algo = Algorithm.HMAC256(config.secret)

    private val issuer: String = config.issuer
    private val audience: String = config.audience
    val expiresSeconds: Long = config.expiresSeconds

    fun verifier(): JWTVerifier = JWT
        .require(algo)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun createToken(userId: String): String {
        val now = System.currentTimeMillis()
        val exp = Date(now + expiresSeconds * 1000)
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withIssuedAt(Date(now))
            .withExpiresAt(exp)
            .withSubject(userId)
            .sign(algo)
    }
}
