-- Add tag column to prds table for grouping PRDs into implementation batches
ALTER TABLE prds ADD COLUMN tag VARCHAR(100);

-- Index for efficient filtering by tag
CREATE INDEX idx_prds_tag ON prds(tag) WHERE tag IS NOT NULL;
