package com.repcheck.common.extensions

import com.repcheck.common.exceptions.UnauthorizedException
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

/**
 * Extension function to get the authenticated user ID from JWT
 * @throws UnauthorizedException if the user is not authenticated
 */
fun ApplicationCall.getUserId(): Long =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
        ?: throw UnauthorizedException()
