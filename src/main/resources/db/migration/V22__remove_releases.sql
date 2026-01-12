-- V22__remove_releases.sql
-- Remove release feature from the database

-- Remove foreign key constraint from epics table
ALTER TABLE epics DROP CONSTRAINT IF EXISTS fk_epics_release_id;

-- Drop release_id column from epics table
ALTER TABLE epics DROP COLUMN IF EXISTS release_id;

-- Drop releases table
DROP TABLE IF EXISTS releases;
