-- ============================================================
-- V11__add_waiter_id_to_tables.sql
-- Add waiter_id column to restaurant_tables for Waiter Data Isolation
-- ============================================================

ALTER TABLE restaurant_tables ADD COLUMN IF NOT EXISTS waiter_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_restaurant_tables_waiter ON restaurant_tables(waiter_id);
