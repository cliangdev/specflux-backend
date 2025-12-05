-- Add local_path column to projects table for storing local filesystem path
ALTER TABLE projects ADD COLUMN local_path VARCHAR(1000);
