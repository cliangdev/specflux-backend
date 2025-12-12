-- Add IMPLEMENTED status to PRD status check constraint
ALTER TABLE prds DROP CONSTRAINT chk_prd_status;
ALTER TABLE prds ADD CONSTRAINT chk_prd_status CHECK (status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'IMPLEMENTED', 'ARCHIVED'));
