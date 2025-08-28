-- V2__add_workout_tables.sql
CREATE TABLE IF NOT EXISTS workout_videos (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    s3_key TEXT NOT NULL,
    s3_bucket TEXT NOT NULL,
    status TEXT NOT NULL,
    duration_seconds INT,
    file_size_bytes BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_workout_videos_user_id ON workout_videos(user_id);
CREATE INDEX IF NOT EXISTS idx_workout_videos_status ON workout_videos(status);