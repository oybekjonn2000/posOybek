-- ============================================================
-- V3__seed_roles_users_and_tables.sql
-- Complete required roles, users, tables and products
-- ============================================================

-- 1. ROLES
INSERT INTO roles (id, tenant_id, name, description, is_system, is_active)
VALUES 
    ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'MANAGER', 'Restoran Menejeri', TRUE, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Grant ALL permissions to MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'b0000000-0000-0000-0000-000000000005', id FROM permissions
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Grant permissions to KITCHEN
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'b0000000-0000-0000-0000-000000000004', id FROM permissions 
WHERE code IN ('KITCHEN_VIEW', 'KITCHEN_UPDATE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 2. USERS (manager, kitchen)
INSERT INTO users (id, tenant_id, username, email, phone, password_hash, first_name, last_name, pin_hash, language, is_active)
VALUES 
    (
        'c0000000-0000-0000-0000-000000000004',
        'a0000000-0000-0000-0000-000000000001',
        'kitchen',
        'kitchen@restaurantpos.uz',
        '+998904445566',
        crypt('kitchen123', gen_salt('bf', 10)),
        'Bobur',
        'Oshpaz',
        crypt('4444', gen_salt('bf', 10)),
        'uz',
        TRUE
    ),
    (
        'c0000000-0000-0000-0000-000000000005',
        'a0000000-0000-0000-0000-000000000001',
        'manager',
        'manager@restaurantpos.uz',
        '+998905556677',
        crypt('manager123', gen_salt('bf', 10)),
        'Anvar',
        'Menejer',
        crypt('5555', gen_salt('bf', 10)),
        'uz',
        TRUE
    )
ON CONFLICT (id) DO UPDATE SET 
    password_hash = EXCLUDED.password_hash,
    pin_hash = EXCLUDED.pin_hash,
    is_active = TRUE;

-- Update admin, cashier, waiter passwords to match spec
UPDATE users SET password_hash = crypt('admin123', gen_salt('bf', 10)), pin_hash = crypt('1111', gen_salt('bf', 10)) WHERE username = 'admin';
UPDATE users SET password_hash = crypt('cashier123', gen_salt('bf', 10)), pin_hash = crypt('2222', gen_salt('bf', 10)) WHERE username = 'cashier';
UPDATE users SET password_hash = crypt('waiter123', gen_salt('bf', 10)), pin_hash = crypt('3333', gen_salt('bf', 10)) WHERE username = 'waiter';

-- User Roles
INSERT INTO user_roles (user_id, role_id) VALUES
    ('c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004'), -- kitchen -> KITCHEN
    ('c0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005')  -- manager -> MANAGER
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 3. 10 RESTAURANT TABLES (Stol 1 to Stol 10, all FREE)
INSERT INTO restaurant_tables (id, tenant_id, zone_id, table_number, name, capacity, shape, pos_x, pos_y, width, height, status, is_active)
VALUES
    ('e1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '1', 'Stol 1', 4, 'rectangle', 40, 40, 100, 80, 'FREE', TRUE),
    ('e1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '2', 'Stol 2', 4, 'rectangle', 180, 40, 100, 80, 'FREE', TRUE),
    ('e1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '3', 'Stol 3', 6, 'rectangle', 320, 40, 120, 80, 'FREE', TRUE),
    ('e1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '4', 'Stol 4', 2, 'round', 460, 40, 80, 80, 'FREE', TRUE),
    ('e1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '5', 'Stol 5', 4, 'rectangle', 40, 160, 100, 80, 'FREE', TRUE),
    ('e1000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '6', 'Stol 6', 6, 'rectangle', 180, 160, 120, 80, 'FREE', TRUE),
    ('e1000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '7', 'Stol 7', 4, 'rectangle', 320, 160, 100, 80, 'FREE', TRUE),
    ('e1000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '8', 'Stol 8', 4, 'rectangle', 460, 160, 100, 80, 'FREE', TRUE),
    ('e1000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '9', 'Stol 9', 8, 'rectangle', 40, 280, 140, 100, 'FREE', TRUE),
    ('e1000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '10', 'Stol 10', 10, 'rectangle', 200, 280, 160, 100, 'FREE', TRUE)
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name,
    status = 'FREE',
    is_active = TRUE;

-- 4. CATEGORIES
INSERT INTO categories (id, tenant_id, name, name_uz, name_ru, icon, color, sort_order, is_active)
VALUES
    ('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Taomlar', 'Taomlar', 'Основные блюда', 'utensils', '#f59e0b', 1, TRUE),
    ('f0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Salatlar', 'Salatlar', 'Салаты', 'salad', '#10b981', 2, TRUE),
    ('f0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Fast Food', 'Fast Food', 'Фастфуд', 'flame', '#ef4444', 3, TRUE),
    ('f0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Ichimliklar', 'Ichimliklar', 'Напитки', 'coffee', '#3b82f6', 4, TRUE),
    ('f0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'Desertlar', 'Desertlar', 'Десерты', 'cake', '#ec4899', 5, TRUE)
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name,
    name_uz = EXCLUDED.name_uz,
    is_active = TRUE;

-- 5. PRODUCTS (Osh, Lag‘mon, Manti, Shashlik, Sho‘rva, Salat, Cola, Fanta, Choy, Suv, etc.)
INSERT INTO products (id, tenant_id, category_id, sku, barcode, name, name_uz, name_ru, unit, purchase_price, sale_price, is_active, is_available, track_stock, sort_order)
VALUES
    ('a1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'DSH-001', '2000001', 'Osh (To''y Oshi)', 'Osh (To''y Oshi)', 'Плов праздничный', 'portion', 25000.00, 45000.00, TRUE, TRUE, FALSE, 1),
    ('a1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'DSH-002', '2000002', 'Lag''mon (Qovurma)', 'Lag''mon (Qovurma)', 'Жареный лагман', 'portion', 20000.00, 38000.00, TRUE, TRUE, FALSE, 2),
    ('a1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'DSH-003', '2000003', 'Manti (5 dona)', 'Manti (5 dona)', 'Манты (5 шт)', 'portion', 22000.00, 40000.00, TRUE, TRUE, FALSE, 3),
    ('a1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'DSH-004', '2000004', 'Shashlik (Qo''y Go''shti)', 'Shashlik (Qo''y Go''shti)', 'Шашлык из баранины', 'piece', 12000.00, 22000.00, TRUE, TRUE, FALSE, 4),
    ('a1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'DSH-005', '2000005', 'Sho''rva (Ko''za)', 'Sho''rva (Ko''za)', 'Шурпа в горшочке', 'portion', 18000.00, 35000.00, TRUE, TRUE, FALSE, 5),

    ('a1000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000002', 'SLD-001', '2000006', 'Achichuk Salat', 'Achichuk Salat', 'Ачичук', 'portion', 8000.00, 18000.00, TRUE, TRUE, FALSE, 1),
    ('a1000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000002', 'SLD-002', '2000007', 'Sezar Salati', 'Sezar Salati', 'Салат Цезарь', 'portion', 18000.00, 35000.00, TRUE, TRUE, FALSE, 2),

    ('a1000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000003', 'FST-001', '2000008', 'Burger Klassik', 'Burger Klassik', 'Бургер классический', 'piece', 15000.00, 30000.00, TRUE, TRUE, FALSE, 1),
    ('a1000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000003', 'FST-002', '2000009', 'Lavash Mol Go''shti', 'Lavash Mol Go''shti', 'Лаваш с говядиной', 'piece', 16000.00, 32000.00, TRUE, TRUE, FALSE, 2),

    ('a1000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000004', 'DRK-001', '2000010', 'Ko''k Choy (Choynak)', 'Ko''k Choy (Choynak)', 'Чай зеленый', 'pot', 2000.00, 8000.00, TRUE, TRUE, FALSE, 1),
    ('a1000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000004', 'DRK-002', '2000011', 'Coca-Cola 1.5L', 'Coca-Cola 1.5L', 'Coca-Cola 1.5л', 'bottle', 11000.00, 16000.00, TRUE, TRUE, FALSE, 2),
    ('a1000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000004', 'DRK-003', '2000012', 'Fanta 1.5L', 'Fanta 1.5L', 'Fanta 1.5л', 'bottle', 11000.00, 16000.00, TRUE, TRUE, FALSE, 3),
    ('a1000000-0000-0000-0000-000000000013', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000004', 'DRK-004', '2000013', 'Toza Suv 0.5L', 'Toza Suv 0.5L', 'Вода без газа', 'bottle', 2000.00, 5000.00, TRUE, TRUE, FALSE, 4),

    ('a1000000-0000-0000-0000-000000000014', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000005', 'DST-001', '2000014', 'Turk Paxlavasi', 'Turk Paxlavasi', 'Пахлава', 'portion', 14000.00, 30000.00, TRUE, TRUE, FALSE, 1),
    ('a1000000-0000-0000-0000-000000000015', 'a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000005', 'DST-002', '2000015', 'Klassik Medovik', 'Klassik Medovik', 'Медовик', 'portion', 12000.00, 25000.00, TRUE, TRUE, FALSE, 2)
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name,
    sale_price = EXCLUDED.sale_price,
    is_active = TRUE,
    is_available = TRUE;
