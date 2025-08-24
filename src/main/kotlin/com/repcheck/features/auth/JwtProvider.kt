package com.repcheck.features.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.repcheck.config.AppConfig
import java.util.Date

class JwtProvider {
    private val cfg = AppConfig.jwtConfig()
    private val algo = Algorithm.HMAC256(cfg.secret)

    private val issuer: String = cfg.issuer
    private val audience: String = cfg.audience
    val expiresSeconds: Long = cfg.expiresSeconds

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
