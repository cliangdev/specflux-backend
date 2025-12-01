-- Epics for large feature groupings
CREATE TABLE epics (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(24) NOT NULL UNIQUE,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    release_id BIGINT REFERENCES releases(id) ON DELETE SET NULL,
    sequence_number INTEGER NOT NULL,
    display_key VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'planning',
    target_date DATE,
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_epic_display_key UNIQUE (project_id, display_key),
    CONSTRAINT chk_epic_status CHECK (status IN ('planning', 'in_progress', 'blocked', 'completed', 'cancelled'))
);

CREATE INDEX idx_epics_project_id ON epics(project_id);
CREATE INDEX idx_epics_release_id ON epics(release_id);
