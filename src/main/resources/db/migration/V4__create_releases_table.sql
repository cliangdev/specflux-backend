-- Releases/milestones for project roadmap
CREATE TABLE releases (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(24) NOT NULL UNIQUE,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    display_key VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    target_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'planned',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_release_display_key UNIQUE (project_id, display_key),
    CONSTRAINT chk_release_status CHECK (status IN ('planned', 'in_progress', 'released', 'cancelled'))
);

CREATE INDEX idx_releases_project_id ON releases(project_id);
