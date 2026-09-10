-- ============================================================
-- V10__minimal_test_seed.sql
-- Minimal seed data strictly for testing the POS system:
-- 2 Waiters, 2 Kitchens, 4 Categories, 8 Products, 4 Tables
-- ZERO pre-existing orders, items, tickets, payments.
-- ============================================================

-- 1. ADD kitchen_id COLUMN TO users TABLE
ALTER TABLE users ADD COLUMN IF NOT EXISTS kitchen_id UUID REFERENCES kitchens(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_users_kitchen ON users(kitchen_id);

-- 2. PURGE PREVIOUS ORDERS, ITEMS, TICKETS, PAYMENTS (INITIAL COUNT = 0)
UPDATE restaurant_tables SET status = 'FREE', current_order_id = NULL;

DELETE FROM order_item_modifiers;
DELETE FROM cancellation_receipts;
DELETE FROM kitchen_tickets;
DELETE FROM payments;
DELETE FROM order_items;
DELETE FROM orders;

-- 3. KITCHENS — EXACTLY 2 TEST KITCHENS ACTIVE
-- Deactivate any other kitchens
UPDATE kitchens SET is_active = FALSE WHERE code NOT IN ('PIZZA', 'SOMSA');

-- Upsert Pitsaxona
INSERT INTO kitchens (id, tenant_id, name, code, description, sort_order, is_active, created_at, updated_at)
VALUES (
    'd0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'Pitsaxona',
    'PIZZA',
    'Pitsalar va pizza ichimliklari stansiyasi',
    1,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Upsert Somsapaz
INSERT INTO kitchens (id, tenant_id, name, code, description, sort_order, is_active, created_at, updated_at)
VALUES (
    'd0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'Somsapaz',
    'SOMSA',
    'Somsalar va somsa ichimliklari stansiyasi',
    2,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- 4. CATEGORIES — EXACTLY 4 CATEGORIES (2 per kitchen)
-- Deactivate other categories
UPDATE categories SET is_active = FALSE WHERE id NOT IN (
    'f0000000-0000-0000-0000-000000000013',
    'f0000000-0000-0000-0000-000000000021',
    'f0000000-0000-0000-0000-000000000012',
    'f0000000-0000-0000-0000-000000000022'
);

-- Pitsaxona -> 1. Pitsalar
INSERT INTO categories (id, tenant_id, kitchen_id, name, description, icon, color, sort_order, is_active, created_at, updated_at)
VALUES (
    'f0000000-0000-0000-0000-000000000013',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000003',
    'Pitsalar',
    'Pitsaxona taomlari',
    'local_pizza',
    '#E65100',
    1,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    kitchen_id = EXCLUDED.kitchen_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    icon = EXCLUDED.icon,
    color = EXCLUDED.color,
    sort_order = EXCLUDED.sort_order,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Pitsaxona -> 2. Pizza ichimliklari
INSERT INTO categories (id, tenant_id, kitchen_id, name, description, icon, color, sort_order, is_active, created_at, updated_at)
VALUES (
    'f0000000-0000-0000-0000-000000000021',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000003',
    'Pizza ichimliklari',
    'Pitsaxona ichimliklari',
    'local_drink',
    '#D84315',
    2,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    kitchen_id = EXCLUDED.kitchen_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    icon = EXCLUDED.icon,
    color = EXCLUDED.color,
    sort_order = EXCLUDED.sort_order,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Somsapaz -> 3. Somsalar
INSERT INTO categories (id, tenant_id, kitchen_id, name, description, icon, color, sort_order, is_active, created_at, updated_at)
VALUES (
    'f0000000-0000-0000-0000-000000000012',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000002',
    'Somsalar',
    'Somsapaz tandir va qandolat somsalar',
    'bakery_dining',
    '#F57C00',
    3,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    kitchen_id = EXCLUDED.kitchen_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    icon = EXCLUDED.icon,
    color = EXCLUDED.color,
    sort_order = EXCLUDED.sort_order,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Somsapaz -> 4. Somsa ichimliklari
INSERT INTO categories (id, tenant_id, kitchen_id, name, description, icon, color, sort_order, is_active, created_at, updated_at)
VALUES (
    'f0000000-0000-0000-0000-000000000022',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000002',
    'Somsa ichimliklari',
    'Somsapaz choy va ichimliklari',
    'emoji_food_beverage',
    '#EF6C00',
    4,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    kitchen_id = EXCLUDED.kitchen_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    icon = EXCLUDED.icon,
    color = EXCLUDED.color,
    sort_order = EXCLUDED.sort_order,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- 5. PRODUCTS — EXACTLY 8 TEST PRODUCTS (2 per category)
-- Avoid SKU / Barcode unique collisions with old non-test products
UPDATE products 
SET sku = 'OLD-' || id::text, barcode = 'OLD-' || id::text, is_active = FALSE
WHERE id NOT IN (
    'a1000000-0000-0000-0000-000000000031',
    'a1000000-0000-0000-0000-000000000032',
    'a1000000-0000-0000-0000-000000000033',
    'a1000000-0000-0000-0000-000000000034',
    'a1000000-0000-0000-0000-000000000035',
    'a1000000-0000-0000-0000-000000000036',
    'a1000000-0000-0000-0000-000000000037',
    'a1000000-0000-0000-0000-000000000038'
);

-- Pitsalar: 1. Margarita Pizza (45000)
INSERT INTO products (id, tenant_id, category_id, kitchen_id, sku, barcode, name, unit, purchase_price, sale_price, is_taxable, is_active, is_available, track_stock, current_stock, sort_order, created_at, updated_at)
VALUES (
    'a1000000-0000-0000-0000-000000000031',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000013',
    'd0000000-0000-0000-0000-000000000003',
    'PIZ-001', '200000000031', 'Margarita Pizza', 'piece', 25000.00, 45000.00, FALSE, TRUE, TRUE, FALSE, 100, 1, NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    kitchen_id = EXCLUDED.kitchen_id,
    sku = EXCLUDED.sku,
    name = EXCLUDED.name,
    sale_price = EXCLUDED.sale_price,
    is_active = TRUE,
    is_available = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Pitsalar: 2. Pepperoni Pizza (55000)
INSERT INTO products (id, tenant_id, category_id, kitchen_id, sku, barcode, name, unit, purchase_price, sale_price, is_taxable, is_active, is_available, track_stock, current_stock, sort_order, created_at, updated_at)
VALUES (
    'a1000000-0000-0000-0000-000000000032',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000013',
    'd0000000-0000-0000-0000-000000000003',
    'PIZ-002', '200000000032', 'Pepperoni Pizza', 'piece', 30000.00, 55000.00, FALSE, TRUE, TRUE, FALSE, 100, 2, NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    kitchen_id = EXCLUDED.kitchen_id,
    sku = EXCLUDED.sku,
    name = EXCLUDED.name,
    sale_price = EXCLUDED.sale_price,
    is_active = TRUE,
    is_available = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Pizza ichimliklari: 3. Coca-Cola (12000)
INSERT INTO products (id, tenant_id, category_id, kitchen_id, sku, barcode, name, unit, purchase_price, sale_price, is_taxable, is_active, is_available, track_stock, current_stock, sort_order, created_at, updated_at)
VALUES (
    'a1000000-0000-0000-0000-000000000033',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000021',
    'd0000000-0000-0000-0000-000000000003',
    'PIZ-003', '200000000033', 'Coca-Cola', 'piece', 8000.00, 12000.00, FALSE, TRUE, TRUE, FALSE, 100, 3, NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    kitchen_id = EXCLUDED.kitchen_id,
    sku = EXCLUDED.sku,
    name = EXCLUDED.name,
    sale_price = EXCLUDED.sale_price,
    is_active = TRUE,
    is_available = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Pizza ichimliklari: 4. Fanta (12000)
INSERT INTO products (id, tenant_id, category_id, kitchen_id, sku, barcode, name, unit, purchase_price, sale_price, is_taxable, is_active, is_available, track_stock, current_stock, sort_order, created_at, updated_at)
VALUES (
    'a1000000-0000-0000-0000-000000000034',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000021',
    'd0000000-0000-0000-0000-000000000003',
    'PIZ-004', '200000000034', 'Fanta', 'piece', 8000.00, 12000.00, FALSE, TRUE, TRUE, FALSE, 100, 4, NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    kitchen_id = EXCLUDED.kitchen_id,
    sku = EXCLUDED.sku,
    name = EXCLUDED.name,
    sale_price = EXCLUDED.sale_price,
    is_active = TRUE,
    is_available = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Somsalar: 5. Go‘shtli Somsa (15000)
INSERT INTO products (id, tenant_id, category_id, kitchen_id, sku, barcode, name, unit, purchase_price, sale_price, is_taxable, is_active, is_available, track_stock, current_stock, sort_order, created_at, updated_at)
VALUES (
    'a1000000-0000-0000-0000-000000000035',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000012',
    'd0000000-0000-0000-0000-000000000002',
    'SOM-001', '200000000035', 'Go‘shtli Somsa', 'piece', 10000.00, 15000.00, FALSE, TRUE, TRUE, FALSE, 100, 5, NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    kitchen_id = EXCLUDED.kitchen_id,
    sku = EXCLUDED.sku,
    name = EXCLUDED.name,
    sale_price = EXCLUDED.sale_price,
    is_active = TRUE,
    is_available = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Somsalar: 6. Pishloqli Somsa (18000)
INSERT INTO products (id, tenant_id, category_id, kitchen_id, sku, barcode, name, unit, purchase_price, sale_price, is_taxable, is_active, is_available, track_stock, current_stock, sort_order, created_at, updated_at)
VALUES (
    'a1000000-0000-0000-0000-000000000036',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000012',
    'd0000000-0000-0000-0000-000000000002',
    'SOM-002', '200000000036', 'Pishloqli Somsa', 'piece', 11000.00, 18000.00, FALSE, TRUE, TRUE, FALSE, 100, 6, NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    kitchen_id = EXCLUDED.kitchen_id,
    sku = EXCLUDED.sku,
    name = EXCLUDED.name,
    sale_price = EXCLUDED.sale_price,
    is_active = TRUE,
    is_available = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Somsa ichimliklari: 7. Choy (5000)
INSERT INTO products (id, tenant_id, category_id, kitchen_id, sku, barcode, name, unit, purchase_price, sale_price, is_taxable, is_active, is_available, track_stock, current_stock, sort_order, created_at, updated_at)
VALUES (
    'a1000000-0000-0000-0000-000000000037',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000022',
    'd0000000-0000-0000-0000-000000000002',
    'SOM-003', '200000000037', 'Choy', 'piece', 2000.00, 5000.00, FALSE, TRUE, TRUE, FALSE, 100, 7, NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    kitchen_id = EXCLUDED.kitchen_id,
    sku = EXCLUDED.sku,
    name = EXCLUDED.name,
    sale_price = EXCLUDED.sale_price,
    is_active = TRUE,
    is_available = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Somsa ichimliklari: 8. Ayran (10000)
INSERT INTO products (id, tenant_id, category_id, kitchen_id, sku, barcode, name, unit, purchase_price, sale_price, is_taxable, is_active, is_available, track_stock, current_stock, sort_order, created_at, updated_at)
VALUES (
    'a1000000-0000-0000-0000-000000000038',
    'a0000000-0000-0000-0000-000000000001',
    'f0000000-0000-0000-0000-000000000022',
    'd0000000-0000-0000-0000-000000000002',
    'SOM-004', '200000000038', 'Ayran', 'piece', 5000.00, 10000.00, FALSE, TRUE, TRUE, FALSE, 100, 8, NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    kitchen_id = EXCLUDED.kitchen_id,
    sku = EXCLUDED.sku,
    name = EXCLUDED.name,
    sale_price = EXCLUDED.sale_price,
    is_active = TRUE,
    is_available = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- 6. TABLES — EXACTLY 4 ACTIVE TABLES (ALL FREE)
UPDATE restaurant_tables SET is_active = FALSE WHERE table_number NOT IN ('1', '2', '3', '4');

UPDATE restaurant_tables 
SET name = 'Stol 1', status = 'FREE', current_order_id = NULL, is_active = TRUE 
WHERE table_number = '1';

UPDATE restaurant_tables 
SET name = 'Stol 2', status = 'FREE', current_order_id = NULL, is_active = TRUE 
WHERE table_number = '2';

UPDATE restaurant_tables 
SET name = 'Stol 3', status = 'FREE', current_order_id = NULL, is_active = TRUE 
WHERE table_number = '3';

UPDATE restaurant_tables 
SET name = 'Stol 4', status = 'FREE', current_order_id = NULL, is_active = TRUE 
WHERE table_number = '4';

-- 7. USERS — TEST USERS (admin, waiter1, waiter2, pizza, somsa)
-- Deactivate all other users
UPDATE users SET is_active = FALSE WHERE username NOT IN ('admin', 'waiter1', 'waiter2', 'pizza', 'somsa');
UPDATE users SET is_active = TRUE WHERE username = 'admin';

-- User 1: waiter1 / waiter123 (Role: WAITER)
INSERT INTO users (id, tenant_id, kitchen_id, username, email, phone, password_hash, first_name, last_name, language, is_active, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000011',
    'a0000000-0000-0000-0000-000000000001',
    NULL,
    'waiter1',
    'waiter1@restaurantpos.uz',
    '+998901000001',
    crypt('waiter123', gen_salt('bf', 10)),
    'Ofitsiant',
    'Birinchi',
    'uz',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    password_hash = crypt('waiter123', gen_salt('bf', 10)),
    kitchen_id = NULL,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- User 2: waiter2 / waiter123 (Role: WAITER)
INSERT INTO users (id, tenant_id, kitchen_id, username, email, phone, password_hash, first_name, last_name, language, is_active, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000012',
    'a0000000-0000-0000-0000-000000000001',
    NULL,
    'waiter2',
    'waiter2@restaurantpos.uz',
    '+998901000002',
    crypt('waiter123', gen_salt('bf', 10)),
    'Ofitsiant',
    'Ikkinchi',
    'uz',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    password_hash = crypt('waiter123', gen_salt('bf', 10)),
    kitchen_id = NULL,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- User 3: pizza / pizza123 (Role: KITCHEN, kitchen: Pitsaxona)
INSERT INTO users (id, tenant_id, kitchen_id, username, email, phone, password_hash, first_name, last_name, language, is_active, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000013',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000003',
    'pizza',
    'pizza@restaurantpos.uz',
    '+998901000003',
    crypt('pizza123', gen_salt('bf', 10)),
    'Pitsaxona',
    'Oshpaz',
    'uz',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    password_hash = crypt('pizza123', gen_salt('bf', 10)),
    kitchen_id = EXCLUDED.kitchen_id,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- User 4: somsa / somsa123 (Role: KITCHEN, kitchen: Somsapaz)
INSERT INTO users (id, tenant_id, kitchen_id, username, email, phone, password_hash, first_name, last_name, language, is_active, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000014',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000002',
    'somsa',
    'somsa@restaurantpos.uz',
    '+998901000004',
    crypt('somsa123', gen_salt('bf', 10)),
    'Somsapaz',
    'Oshpaz',
    'uz',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    password_hash = crypt('somsa123', gen_salt('bf', 10)),
    kitchen_id = EXCLUDED.kitchen_id,
    is_active = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

-- Assign Roles in user_roles
-- WAITER role: 'b0000000-0000-0000-0000-000000000003'
INSERT INTO user_roles (user_id, role_id)
VALUES 
    ('c0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000003'),
    ('c0000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000003')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- KITCHEN role: 'b0000000-0000-0000-0000-000000000004'
INSERT INTO user_roles (user_id, role_id)
VALUES 
    ('c0000000-0000-0000-0000-000000000013', 'b0000000-0000-0000-0000-000000000004'),
    ('c0000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000004')
ON CONFLICT (user_id, role_id) DO NOTHING;
