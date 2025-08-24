-- Add email verification fields to users table
-- Using simple ALTER TABLE statements that work in both PostgreSQL and H2

-- Add is_verified column
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT false;

-- Add verification_token column
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255);

-- Add verification_token_expires_at column
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token_expires_at TIMESTAMP WITH TIME ZONE;

-- Create an index on verification_token for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_verification_token ON users(verification_token);

-- Update existing users to be marked as verified (if this is a production system, you might want to handle this differently)
-- UPDATE users SET is_verified = true;  -- Uncomment and run manually if needed for existing users
