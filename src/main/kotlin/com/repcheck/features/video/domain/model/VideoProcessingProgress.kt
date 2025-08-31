package com.repcheck.features.video.domain.model

object VideoProcessingProgress {
    // Initial validation and setup
    const val INITIAL = 0
    const val VALIDATION_COMPLETE = 10
    
    // Feature extraction
    const val FEATURE_EXTRACTION_START = 20
    const val FEATURE_EXTRACTION_COMPLETE = 40
    
    // Workout analysis
    const val ANALYSIS_START = 50
    const val ANALYSIS_COMPLETE = 70
    
    // Report generation
    const val REPORT_GENERATION_START = 80
    const val REPORT_GENERATION_COMPLETE = 90
    
    // Final processing
    const val COMPLETE = 100
}
