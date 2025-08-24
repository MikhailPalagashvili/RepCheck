package com.repcheck.features.workout.domain.model

enum class LiftType {
    SQUAT,
    BENCH_PRESS,
    DEADLIFT,
    OVERHEAD_PRESS;

    companion object {
        fun fromString(value: String): LiftType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

data class Lift(
    val id: Long = 0,
    val type: LiftType,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
