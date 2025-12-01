-- Tasks for individual work items
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(24) NOT NULL UNIQUE,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    epic_id BIGINT REFERENCES epics(id) ON DELETE SET NULL,
    sequence_number INTEGER NOT NULL,
    display_key VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'backlog',
    priority VARCHAR(10) DEFAULT 'medium',
    requires_approval BOOLEAN DEFAULT true,
    estimated_duration INTEGER,
    actual_duration INTEGER,
    github_pr_url VARCHAR(500),
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    assigned_to_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_task_display_key UNIQUE (project_id, display_key),
    CONSTRAINT chk_task_status CHECK (status IN ('backlog', 'ready', 'in_progress', 'in_review', 'blocked', 'completed', 'cancelled')),
    CONSTRAINT chk_task_priority CHECK (priority IN ('low', 'medium', 'high', 'critical'))
);

CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_epic_id ON tasks(epic_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_assigned_to ON tasks(assigned_to_id);
