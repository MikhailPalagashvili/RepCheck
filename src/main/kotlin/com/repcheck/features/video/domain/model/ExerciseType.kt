package com.repcheck.features.video.domain.model

/**
 * Enum representing the four main barbell lifts with their Starting Strength form standards.
 */
enum class ExerciseType {
    SQUAT {
        override fun displayName(): String = "Squat"
        override fun keyPoints(): List<String> = listOf(
            "Feet shoulder-width apart, toes out at 30 degrees",
            "Bar positioned on upper back (low-bar position)",
            "Knees track over toes",
            "Hips back and down, break at hips first",
            "Descend until hip crease is below top of knee",
            "Drive up through mid-foot"
        )
    },
    BENCH_PRESS {
        override fun displayName(): String = "Bench Press"
        override fun keyPoints(): List<String> = listOf(
            "Lay on bench with eyes under the bar",
            "Grip slightly wider than shoulder-width",
            "Feet flat on the floor",
            "Arch in lower back, chest up",
            "Bar touches mid-chest (nipple line)",
            "Forearms vertical at bottom"
        )
    },
    DEADLIFT {
        override fun displayName(): String = "Deadlift"
        override fun keyPoints(): List<String> = listOf(
            "Feet hip-width, bar over mid-foot",
            "Grip just outside knees",
            "Shoulder blades directly over the bar",
            "Chest up, back flat",
            "Drag bar up legs",
            "Hips and shoulders rise at same rate"
        )
    },
    OVERHEAD_PRESS {
        override fun displayName(): String = "Overhead Press"
        override fun keyPoints(): List<String> = listOf(
            "Grip just outside shoulders",
            "Elbows in front of the bar",
            "Forearms vertical",
            "Slight layback at the start",
            "Press bar up in an arc to lockout",
            "Head moves forward as bar passes"
        )
    };

    abstract fun displayName(): String
    abstract fun keyPoints(): List<String>

    companion object {
        fun fromString(value: String): ExerciseType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
