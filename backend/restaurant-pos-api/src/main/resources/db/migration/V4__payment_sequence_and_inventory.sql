-- ============================================================
-- V4: Payment Sequence + Inventory Tables
-- ============================================================

-- Payment number sequence (server restart safe, cluster safe)
CREATE SEQUENCE IF NOT EXISTS payment_number_seq
    START WITH 1001
    INCREMENT BY 1
    NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS order_number_seq
    START WITH 1001
    INCREMENT BY 1
    NO CYCLE;

-- ============================================================
-- INVENTORY
-- ============================================================

CREATE TABLE inventory_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    sku             VARCHAR(100),
    unit            VARCHAR(50) NOT NULL DEFAULT 'pcs',
    quantity        NUMERIC(15, 3) NOT NULL DEFAULT 0,
    min_quantity    NUMERIC(15, 3) NOT NULL DEFAULT 0,
    cost_price      NUMERIC(15, 2),
    category        VARCHAR(100),
    notes           TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    UNIQUE(tenant_id, sku)
);

CREATE INDEX idx_inventory_tenant ON inventory_items(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_low_stock ON inventory_items(tenant_id, quantity, min_quantity) WHERE deleted_at IS NULL;

-- Inventory transaction log (who added/removed what and when)
CREATE TABLE inventory_transactions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    item_id         UUID NOT NULL REFERENCES inventory_items(id),
    type            VARCHAR(30) NOT NULL,   -- PURCHASE, SALE, ADJUSTMENT, WASTE, RETURN
    quantity        NUMERIC(15, 3) NOT NULL,  -- positive = in, negative = out
    quantity_before NUMERIC(15, 3) NOT NULL,
    quantity_after  NUMERIC(15, 3) NOT NULL,
    reference_type  VARCHAR(50),   -- ORDER, MANUAL, etc.
    reference_id    UUID,
    notes           TEXT,
    user_id         UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_tx_item ON inventory_transactions(item_id, created_at DESC);
CREATE INDEX idx_inv_tx_tenant ON inventory_transactions(tenant_id, created_at DESC);

-- Link between products and inventory items (recipe)
CREATE TABLE product_ingredients (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id      UUID NOT NULL REFERENCES products(id),
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    quantity        NUMERIC(15, 3) NOT NULL,  -- amount consumed per 1 unit sold
    unit            VARCHAR(50) NOT NULL DEFAULT 'pcs',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, inventory_item_id)
);

CREATE INDEX idx_product_ingredients_product ON product_ingredients(product_id);
