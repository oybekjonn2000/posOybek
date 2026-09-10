-- V6: Cancellation Receipts and Void Audit System
-- Provides immutable audit trail for full and partial order cancellations

CREATE TABLE IF NOT EXISTS cancellation_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    receipt_number VARCHAR(50) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES orders(id),
    order_number VARCHAR(50) NOT NULL,
    table_id UUID REFERENCES restaurant_tables(id),
    table_name VARCHAR(100),
    cancelled_by UUID REFERENCES users(id),
    cancelled_by_name VARCHAR(150),
    reason TEXT NOT NULL,
    item_id UUID REFERENCES order_items(id),
    item_name VARCHAR(255),
    cancelled_quantity NUMERIC(10, 3),
    unit_price NUMERIC(15, 2),
    total_amount NUMERIC(15, 2) NOT NULL,
    is_full_order BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cancellation_receipts_tenant ON cancellation_receipts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_receipts_order ON cancellation_receipts(order_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_receipts_created ON cancellation_receipts(created_at);
