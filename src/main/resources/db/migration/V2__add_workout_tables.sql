-- V2__add_workout_tables.sql

-- Create enum type for lift types (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lift_type') THEN
        CREATE TYPE lift_type AS ENUM (
            'SQUAT',
            'BENCH_PRESS',
            'DEADLIFT',
            'OVERHEAD_PRESS'
        );
    END IF;
END
$$;

-- Workouts table (only if it doesn't exist)
CREATE TABLE IF NOT EXISTS public.workouts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Workout sets (only if it doesn't exist)
CREATE TABLE IF NOT EXISTS public.workout_sets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workout_id UUID NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
    lift_type lift_type NOT NULL,
    weight_kg DECIMAL(5,2) NOT NULL,
    reps INTEGER NOT NULL,
    rpe DECIMAL(3,1),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Videos (only if it doesn't exist)
CREATE TABLE IF NOT EXISTS videos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workout_set_id UUID REFERENCES workout_sets(id) ON DELETE SET NULL,
    s3_key TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- AI Feedback (only if it doesn't exist)
CREATE TABLE IF NOT EXISTS ai_feedback (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    video_id UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    feedback_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes (only if they don't exist)
DO $$
BEGIN
    -- Index for workouts
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_workouts_user_id'
    ) THEN
        CREATE INDEX idx_workouts_user_id ON workouts(user_id);
    END IF;

    -- Index for workout_sets
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_workout_sets_workout_id'
    ) THEN
        CREATE INDEX idx_workout_sets_workout_id ON workout_sets(workout_id);
    END IF;

    -- Index for videos
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_videos_workout_set_id'
    ) THEN
        CREATE INDEX idx_videos_workout_set_id ON videos(workout_set_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_videos_status'
    ) THEN
        CREATE INDEX idx_videos_status ON videos(status);
    END IF;
END
$$;