-- Add unique constraint on users.email to prevent duplicate accounts
-- First, remove duplicate users keeping only the most recently updated one per email

-- Delete duplicate users, keeping the one with the latest updated_at for each email
DELETE FROM users u1
WHERE EXISTS (
    SELECT 1 FROM users u2
    WHERE u2.email = u1.email
    AND u2.updated_at > u1.updated_at
);

-- Now add the unique constraint
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
