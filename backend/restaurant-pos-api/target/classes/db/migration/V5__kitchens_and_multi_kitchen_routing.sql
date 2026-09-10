-- ========================================================
-- V5: KITCHENS AND MULTI-KITCHEN ROUTING SYSTEM
-- ========================================================

-- 1. Create kitchens table
CREATE TABLE IF NOT EXISTS kitchens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_kitchens_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_kitchens_tenant ON kitchens(tenant_id) WHERE deleted_at IS NULL;

-- 2. Seed initial 5 kitchens for main tenant
INSERT INTO kitchens (id, tenant_id, name, code, description, sort_order, is_active)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Palovchi', 'PLOV', 'Osh va milliy palov turlari', 1, TRUE),
    ('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Somsapaz', 'SOMSA', 'Tandir somsa va pishiriqlar', 2, TRUE),
    ('d0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Pitsaxona', 'PIZZA', 'Pitsa va tezkor taomlar', 3, TRUE),
    ('d0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Bar', 'BAR', 'Ichimliklar, choy, qahva va sharbatlar', 4, TRUE),
    ('d0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'Asosiy oshxona', 'MAIN', 'Lag''mon, sho''rva va asosiy issiq taomlar', 5, TRUE)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    description = EXCLUDED.description,
    is_active = TRUE;

-- 3. Add kitchen_id to products
ALTER TABLE products ADD COLUMN IF NOT EXISTS kitchen_id UUID REFERENCES kitchens(id);
CREATE INDEX IF NOT EXISTS idx_products_kitchen ON products(kitchen_id);

-- 4. Add kitchen_id to order_items
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS kitchen_id UUID REFERENCES kitchens(id);
CREATE INDEX IF NOT EXISTS idx_order_items_kitchen ON order_items(kitchen_id);

-- 5. Seed Somsa and Pizza products if missing
INSERT INTO products (id, tenant_id, category_id, name, sku, purchase_price, sale_price, unit, is_active, is_available)
VALUES
    ('a1000000-0000-0000-0000-000000000021', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'Tandir Somsa', 'SMS-001', 5000, 10000, 'piece', true, true),
    ('a1000000-0000-0000-0000-000000000022', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000003', 'Pizza Margarita', 'PIZ-001', 35000, 65000, 'piece', true, true)
ON CONFLICT (id) DO NOTHING;

-- 6. Link products to respective kitchens
-- Palovchi
UPDATE products SET kitchen_id = 'd0000000-0000-0000-0000-000000000001'
WHERE name ILIKE '%osh%' OR name ILIKE '%palov%';

-- Somsapaz
UPDATE products SET kitchen_id = 'd0000000-0000-0000-0000-000000000002'
WHERE name ILIKE '%somsa%' OR name ILIKE '%samsa%';

-- Pitsaxona
UPDATE products SET kitchen_id = 'd0000000-0000-0000-0000-000000000003'
WHERE name ILIKE '%pitsa%' OR name ILIKE '%pizza%' OR name ILIKE '%burger%' OR name ILIKE '%lavash%';

-- Bar
UPDATE products SET kitchen_id = 'd0000000-0000-0000-0000-000000000004'
WHERE name ILIKE '%choy%' OR name ILIKE '%cola%' OR name ILIKE '%fanta%' OR name ILIKE '%sprite%'
   OR name ILIKE '%suv%' OR name ILIKE '%sharbat%' OR name ILIKE '%kofe%' OR name ILIKE '%coffee%'
   OR name ILIKE '%ichimlik%';

-- Asosiy oshxona: all remaining products
UPDATE products SET kitchen_id = 'd0000000-0000-0000-0000-000000000005'
WHERE kitchen_id IS NULL;

-- 7. Create kitchen_tickets table
CREATE TABLE IF NOT EXISTS kitchen_tickets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    kitchen_id UUID NOT NULL REFERENCES kitchens(id) ON DELETE CASCADE,
    ticket_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ready_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_kitchen_tickets_kitchen_status ON kitchen_tickets(kitchen_id, status);
CREATE INDEX IF NOT EXISTS idx_kitchen_tickets_order ON kitchen_tickets(order_id);
