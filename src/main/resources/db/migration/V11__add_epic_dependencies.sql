-- Create epic dependencies table for tracking epic-to-epic dependencies
CREATE TABLE epic_dependencies (
    id BIGSERIAL PRIMARY KEY,
    epic_id BIGINT NOT NULL REFERENCES epics(id) ON DELETE CASCADE,
    depends_on_epic_id BIGINT NOT NULL REFERENCES epics(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_epic_dependency UNIQUE(epic_id, depends_on_epic_id),
    CONSTRAINT no_self_dependency CHECK (epic_id != depends_on_epic_id)
);

-- Index for efficient lookups
CREATE INDEX idx_epic_dependencies_epic_id ON epic_dependencies(epic_id);
CREATE INDEX idx_epic_dependencies_depends_on ON epic_dependencies(depends_on_epic_id);
