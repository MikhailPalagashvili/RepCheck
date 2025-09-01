import com.repcheck.features.video.domain.model.ExerciseType
import com.repcheck.features.video.domain.service.analyzer.*

object ExerciseAnalyzerFactory {
    fun createAnalyzer(exerciseType: ExerciseType): ExerciseAnalyzer {
        return when (exerciseType) {
            ExerciseType.SQUAT -> SquatAnalyzer
            ExerciseType.BENCH_PRESS -> BenchPressAnalyzer
            ExerciseType.DEADLIFT -> DeadliftAnalyzer
            ExerciseType.OVERHEAD_PRESS -> OverheadPressAnalyzer
        }
    }

    fun createAllAnalyzers(): Map<ExerciseType, ExerciseAnalyzer> {
        return ExerciseType.values().associateWith { createAnalyzer(it) }
    }
}