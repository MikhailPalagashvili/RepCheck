package com.repcheck.features.workout.domain.model

data class Workout(
    val id: Long = 0,
    val userId: Long,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
