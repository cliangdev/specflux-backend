-- Add unique constraint on users.email to prevent duplicate accounts
-- This enforces that each email can only have one user record
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
