-- ============================================================
-- V17__warehouse_inventory_and_reports_enhancements.sql
-- Ombor va Hisobotlar tizimi uchun ma'lumotlar bazasi kengaytmalari
-- ============================================================

-- 1. DEFAULT OMBORLARNI KIRITISH
INSERT INTO warehouses (id, tenant_id, name, description, is_default, is_active, created_at, updated_at)
VALUES 
    ('e2000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Asosiy ombor', 'Restoranning markaziy oziq-ovqat va xomashyo ombori', TRUE, TRUE, NOW(), NOW()),
    ('e2000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Bar ombori', 'Ichimliklar, sharbatlar va qahva mahsulotlari ombori', FALSE, TRUE, NOW(), NOW()),
    ('e2000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Sovutkich', 'Muzlatilgan go‘sht, baliq va yarim tayyor mahsulotlar', FALSE, TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. INVENTORY_ITEMS JADVALINI KENGAYTIRISH
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS warehouse_id UUID REFERENCES warehouses(id);
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS max_quantity NUMERIC(15, 3) DEFAULT 0;
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS selling_price NUMERIC(15, 2) DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_inventory_warehouse ON inventory_items(warehouse_id);

-- Barcha mavjud mahsulotlarni 'Asosiy ombor'ga biriktirish
UPDATE inventory_items 
SET warehouse_id = 'e2000000-0000-0000-0000-000000000001'
WHERE warehouse_id IS NULL;

-- 3. INVENTORY_TRANSACTIONS JADVALINI KENGAYTIRISH
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS warehouse_id UUID REFERENCES warehouses(id);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(15, 2);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS total_cost NUMERIC(15, 2);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS reference_number VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_inv_tx_warehouse ON inventory_transactions(warehouse_id);

-- 4. PURCHASE_ITEMS JADVALIGA INVENTORY_ITEM_ID QO'SHISH
ALTER TABLE purchase_items ADD COLUMN IF NOT EXISTS inventory_item_id UUID REFERENCES inventory_items(id);
CREATE INDEX IF NOT EXISTS idx_purchase_items_inv_item ON purchase_items(inventory_item_id);

-- 5. INVENTARIZATSIYA (STOCK AUDIT) JADVALLARI
CREATE TABLE IF NOT EXISTS inventory_audits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    notes TEXT,
    total_items_counted INT NOT NULL DEFAULT 0,
    total_difference_cost NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_by UUID REFERENCES users(id),
    completed_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_inv_audits_tenant ON inventory_audits(tenant_id);
CREATE INDEX IF NOT EXISTS idx_inv_audits_warehouse ON inventory_audits(warehouse_id);

CREATE TABLE IF NOT EXISTS inventory_audit_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audit_id UUID NOT NULL REFERENCES inventory_audits(id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    system_quantity NUMERIC(15, 3) NOT NULL,
    actual_quantity NUMERIC(15, 3) NOT NULL,
    difference_quantity NUMERIC(15, 3) NOT NULL,
    cost_price NUMERIC(15, 2) DEFAULT 0.00,
    difference_cost NUMERIC(15, 2) DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inv_audit_items_audit ON inventory_audit_items(audit_id);
CREATE INDEX IF NOT EXISTS idx_inv_audit_items_item ON inventory_audit_items(inventory_item_id);

-- 6. DEFAULT SUPPLIERS
INSERT INTO suppliers (id, tenant_id, name, phone, email, address, contact_person, tax_number, total_debt, is_active, created_at, updated_at)
VALUES 
    ('e3000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Agro Go''sht Ta''minot MCHJ', '+998 90 111 22 33', 'agro@meat.uz', 'Toshkent sh., Qorasuv bozori', 'Botir aka', '300123456', 0.00, TRUE, NOW(), NOW()),
    ('e3000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Oltin Don Mahsulotlari', '+998 91 222 33 44', 'don@un.uz', 'Toshkent vil., Zangiota', 'Sherzod aka', '300234567', 0.00, TRUE, NOW(), NOW()),
    ('e3000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Agro Sabzavot Distribyutsiya', '+998 93 333 44 55', 'sabzavot@agro.uz', 'Qarshi sh., Dehqon bozori', 'Oybek aka', '300345678', 0.00, TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 7. CASHIER VA WAITER UCHUN KERAKLI RUXSATLARNI BIRIKTIRISH
-- CASHIER VIEW_REPORTS huquqi
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'b0000000-0000-0000-0000-000000000002', id FROM permissions WHERE code = 'VIEW_REPORTS'
ON CONFLICT (role_id, permission_id) DO NOTHING;
