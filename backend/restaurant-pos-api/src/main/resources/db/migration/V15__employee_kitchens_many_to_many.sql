-- ============================================================
-- V15__employee_kitchens_many_to_many.sql
-- Restoran POS: Employee <-> Kitchen Many-to-Many Munosabati
-- ============================================================

-- 1. Create employee_kitchens table
CREATE TABLE IF NOT EXISTS employee_kitchens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kitchen_id UUID NOT NULL REFERENCES kitchens(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_employee_kitchens UNIQUE (employee_id, kitchen_id)
);

-- 2. Indexes for fast lookup
CREATE INDEX IF NOT EXISTS idx_employee_kitchens_employee ON employee_kitchens(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_kitchens_kitchen ON employee_kitchens(kitchen_id);
CREATE INDEX IF NOT EXISTS idx_employee_kitchens_tenant ON employee_kitchens(tenant_id);

-- 3. Migrate existing user.kitchen_id relationships safely without data loss
INSERT INTO employee_kitchens (id, tenant_id, employee_id, kitchen_id, created_at)
SELECT 
    uuid_generate_v4(),
    u.tenant_id,
    u.id,
    u.kitchen_id,
    NOW()
FROM users u
WHERE u.kitchen_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM kitchens k WHERE k.id = u.kitchen_id)
ON CONFLICT (employee_id, kitchen_id) DO NOTHING;
