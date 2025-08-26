package com.repcheck.common.exceptions

class NotFoundException(message: String) : Exception(message)
class BadRequestException(message: String) : Exception(message)
class ForbiddenException(message: String) : Exception(message)
class UnauthorizedException(message: String = "Unauthorized") : Exception(message)
