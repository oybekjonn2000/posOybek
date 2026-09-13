-- ============================================================
-- V20: PAGINATION PERFORMANCE INDEXES
-- Optimizes server-side pagination, search and role-isolated filtering
-- ============================================================

-- 1. Orders: Waiter isolation & pagination sorting
CREATE INDEX IF NOT EXISTS idx_orders_waiter_id 
    ON orders(tenant_id, waiter_id) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_orders_created_at_desc 
    ON orders(tenant_id, created_at DESC) 
    WHERE deleted_at IS NULL;

-- 2. Products: Active flag, name search and category filtering
CREATE INDEX IF NOT EXISTS idx_products_search_active 
    ON products(tenant_id, is_active, name) 
    WHERE deleted_at IS NULL;

-- 3. Inventory Transactions: Type filtering & timestamp sorting
CREATE INDEX IF NOT EXISTS idx_inv_tx_type_created_desc 
    ON inventory_transactions(tenant_id, type, created_at DESC);

-- 4. Audit Logs: Reverse chronological order pagination
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at_desc 
    ON audit_logs(created_at DESC);
