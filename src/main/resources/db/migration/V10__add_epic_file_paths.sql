-- Add file path columns to epics table
ALTER TABLE epics ADD COLUMN prd_file_path VARCHAR(500);
ALTER TABLE epics ADD COLUMN epic_file_path VARCHAR(500);
