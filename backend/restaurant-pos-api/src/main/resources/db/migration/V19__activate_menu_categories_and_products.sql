-- ============================================================
-- V19__activate_menu_categories_and_products.sql
-- Fix: Reactivate categories, products, and users that were
-- deactivated in test seed migrations (V10).
-- Restore Burger Klassik, clean up OLD- SKUs and ensure
-- kitchen routing is preserved.
-- ============================================================

-- 1. Reactivate all non-deleted categories
UPDATE categories
SET is_active = TRUE, updated_at = NOW()
WHERE deleted_at IS NULL;

-- 2. Restore Burger Klassik (was soft-deleted in test)
UPDATE products
SET deleted_at = NULL, is_active = TRUE, is_available = TRUE, updated_at = NOW()
WHERE id = 'a1000000-0000-0000-0000-000000000008';

-- 3. Reactivate and mark available all non-deleted products
UPDATE products
SET is_active = TRUE, is_available = TRUE, updated_at = NOW()
WHERE deleted_at IS NULL;

-- 4. Clean up OLD- SKUs and barcodes created in V10
UPDATE products
SET sku = 'SKU-' || SUBSTRING(id::text FROM 33 FOR 4),
    barcode = '200' || SUBSTRING(id::text FROM 33 FOR 4),
    updated_at = NOW()
WHERE sku LIKE 'OLD-%';

-- 5. Ensure all products are mapped to their category's kitchen
UPDATE products p
SET kitchen_id = c.kitchen_id, updated_at = NOW()
FROM categories c
WHERE p.category_id = c.id
  AND c.kitchen_id IS NOT NULL
  AND (p.kitchen_id IS NULL OR p.kitchen_id != c.kitchen_id);

-- 6. Reactivate all users (admin, manager, waiter, cashier, kitchen)
UPDATE users
SET is_active = TRUE, updated_at = NOW()
WHERE deleted_at IS NULL;
