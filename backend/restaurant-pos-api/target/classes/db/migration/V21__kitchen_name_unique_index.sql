-- ============================================================
-- V21: KITCHEN UNIQUE ACTIVE NAME AND PERFORMANCE INDEXES
-- Enforces case-insensitive unique names per tenant and fast active filtering
-- ============================================================

-- 1. Unique active/non-deleted kitchen name per tenant (case-insensitive)
CREATE UNIQUE INDEX IF NOT EXISTS idx_kitchens_tenant_lower_name_unique 
    ON kitchens(tenant_id, LOWER(name)) 
    WHERE deleted_at IS NULL;

-- 2. Fast active kitchen lookup for order routing and category binding
CREATE INDEX IF NOT EXISTS idx_kitchens_tenant_active 
    ON kitchens(tenant_id, is_active) 
    WHERE deleted_at IS NULL;
