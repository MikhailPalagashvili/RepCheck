-- Flyway Migration: V1__init.sql
-- Minimal initial schema for RepCheck

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Simple healthcheck table (optional, useful in demos)
CREATE TABLE IF NOT EXISTS healthcheck (
    id INT PRIMARY KEY DEFAULT 1,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
