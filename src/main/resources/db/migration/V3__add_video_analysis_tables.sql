-- V3__add_video_analysis_tables.sql
CREATE TABLE IF NOT EXISTS video_analyses (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL REFERENCES workout_videos(id) ON DELETE CASCADE,
    confidence FLOAT,
    analysis_data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_video_analyses_video_id ON video_analyses(video_id);