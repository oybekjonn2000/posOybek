-- V22: Change kitchen code unique constraint to partial index (ignoring soft-deleted records)
ALTER TABLE kitchens DROP CONSTRAINT IF EXISTS uq_kitchens_tenant_code;

DROP INDEX IF EXISTS uq_kitchens_tenant_code_active;
CREATE UNIQUE INDEX uq_kitchens_tenant_code_active ON kitchens (tenant_id, UPPER(code)) WHERE deleted_at IS NULL;
