-- Acceptance criteria for tasks and epics
CREATE TABLE acceptance_criteria (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT REFERENCES tasks(id) ON DELETE CASCADE,
    epic_id BIGINT REFERENCES epics(id) ON DELETE CASCADE,
    criteria TEXT NOT NULL,
    is_met BOOLEAN DEFAULT false,
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_parent CHECK (
        (task_id IS NOT NULL AND epic_id IS NULL) OR
        (task_id IS NULL AND epic_id IS NOT NULL)
    )
);

CREATE INDEX idx_acceptance_criteria_task ON acceptance_criteria(task_id);
CREATE INDEX idx_acceptance_criteria_epic ON acceptance_criteria(epic_id);
