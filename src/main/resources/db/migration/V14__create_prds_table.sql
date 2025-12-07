-- PRDs table for product requirement documents
CREATE TABLE prds (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(24) NOT NULL UNIQUE,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    display_key VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    folder_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prd_display_key UNIQUE (project_id, display_key),
    CONSTRAINT uk_prd_folder_path UNIQUE (project_id, folder_path),
    CONSTRAINT chk_prd_status CHECK (status IN ('draft', 'in_review', 'approved', 'archived'))
);

CREATE INDEX idx_prds_project_id ON prds(project_id);
CREATE INDEX idx_prds_status ON prds(project_id, status);

-- PRD Documents table for supporting documents within a PRD
CREATE TABLE prd_documents (
    id BIGSERIAL PRIMARY KEY,
    prd_id BIGINT NOT NULL REFERENCES prds(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    document_type VARCHAR(20) NOT NULL DEFAULT 'other',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prd_document_path UNIQUE (prd_id, file_path),
    CONSTRAINT chk_prd_document_type CHECK (document_type IN ('prd', 'wireframe', 'mockup', 'design', 'other'))
);

CREATE INDEX idx_prd_documents_prd_id ON prd_documents(prd_id);

-- Update epics table: add prd_id foreign key (nullable for now, migration will be gradual)
ALTER TABLE epics ADD COLUMN prd_id BIGINT REFERENCES prds(id) ON DELETE SET NULL;
CREATE INDEX idx_epics_prd_id ON epics(prd_id);
