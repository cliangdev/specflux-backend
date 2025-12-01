-- Projects table with sequence counters for display keys
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(24) NOT NULL UNIQUE,
    project_key VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    epic_sequence INTEGER DEFAULT 0,
    task_sequence INTEGER DEFAULT 0,
    release_sequence INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_projects_owner_id ON projects(owner_id);
CREATE INDEX idx_projects_key ON projects(project_key);
