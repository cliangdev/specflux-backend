-- Fix PRD status check constraint to use uppercase values (matching Java enum)
ALTER TABLE prds DROP CONSTRAINT chk_prd_status;
ALTER TABLE prds ADD CONSTRAINT chk_prd_status CHECK (status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'ARCHIVED'));

-- Fix PRD document type check constraint to use uppercase values (matching Java enum)
ALTER TABLE prd_documents DROP CONSTRAINT chk_prd_document_type;
ALTER TABLE prd_documents ADD CONSTRAINT chk_prd_document_type CHECK (document_type IN ('PRD', 'WIREFRAME', 'MOCKUP', 'DESIGN', 'OTHER'));

-- Update default values to uppercase
ALTER TABLE prds ALTER COLUMN status SET DEFAULT 'DRAFT';
ALTER TABLE prd_documents ALTER COLUMN document_type SET DEFAULT 'OTHER';
