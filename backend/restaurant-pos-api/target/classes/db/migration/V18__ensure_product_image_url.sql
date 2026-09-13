-- ============================================================
-- V18__ensure_product_image_url.sql
-- Ensure image_url column exists on products table
-- ============================================================

ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url TEXT;
