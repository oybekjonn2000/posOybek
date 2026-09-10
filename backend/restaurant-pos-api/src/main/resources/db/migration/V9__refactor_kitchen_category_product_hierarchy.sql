-- ============================================================
-- V9__refactor_kitchen_category_product_hierarchy.sql
-- Restoran POS: Kitchen -> Category -> Product Arxitekturasi
-- ============================================================

-- 1. categories jadvaliga kitchen_id ustunini qo'shish
ALTER TABLE categories ADD COLUMN IF NOT EXISTS kitchen_id UUID;

-- 2. Index yaratish
CREATE INDEX IF NOT EXISTS idx_categories_kitchen ON categories(kitchen_id);

-- 3. Mavjud oshxonalarga mos maxsus kategoriyalarni yaratish va mavjudlarini biriktirish

-- 3.1 Palovchi (d0000000-0000-0000-0000-000000000001)
INSERT INTO categories (id, tenant_id, kitchen_id, name, name_uz, name_ru, icon, color, sort_order, is_active)
VALUES (
    'f0000000-0000-0000-0000-000000000011',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'Palovlar',
    'Palovlar',
    'Плов',
    'utensils',
    '#f59e0b',
    1,
    TRUE
) ON CONFLICT (id) DO UPDATE SET kitchen_id = EXCLUDED.kitchen_id;

-- Osh mahsulotlarini Palovchi oshxonasining Palovlar kategoriyasiga biriktirish
UPDATE products
SET category_id = 'f0000000-0000-0000-0000-000000000011'
WHERE kitchen_id = 'd0000000-0000-0000-0000-000000000001' OR name ILIKE '%osh%';

-- 3.2 Somsapaz (d0000000-0000-0000-0000-000000000002)
INSERT INTO categories (id, tenant_id, kitchen_id, name, name_uz, name_ru, icon, color, sort_order, is_active)
VALUES (
    'f0000000-0000-0000-0000-000000000012',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000002',
    'Somsalar',
    'Somsalar',
    'Самса',
    'flame',
    '#ec4899',
    2,
    TRUE
) ON CONFLICT (id) DO UPDATE SET kitchen_id = EXCLUDED.kitchen_id;

-- Somsa mahsulotlarini Somsapaz oshxonasining Somsalar kategoriyasiga biriktirish
UPDATE products
SET category_id = 'f0000000-0000-0000-0000-000000000012'
WHERE kitchen_id = 'd0000000-0000-0000-0000-000000000002' OR name ILIKE '%somsa%';

-- 3.3 Pitsaxona (d0000000-0000-0000-0000-000000000003)
UPDATE categories
SET kitchen_id = 'd0000000-0000-0000-0000-000000000003',
    name = 'Pitsalar & Fast Food',
    name_uz = 'Pitsalar & Fast Food'
WHERE id = 'f0000000-0000-0000-0000-000000000003';

INSERT INTO categories (id, tenant_id, kitchen_id, name, name_uz, name_ru, icon, color, sort_order, is_active)
VALUES (
    'f0000000-0000-0000-0000-000000000013',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000003',
    'Pitsalar',
    'Pitsalar',
    'Пицца',
    'pizza',
    '#ef4444',
    3,
    TRUE
) ON CONFLICT (id) DO UPDATE SET kitchen_id = EXCLUDED.kitchen_id;

-- Pizza mahsulotlarini Pitsalar kategoriyasiga, Burger/Lavashni Fast Food ga biriktirish
UPDATE products
SET category_id = 'f0000000-0000-0000-0000-000000000013'
WHERE name ILIKE '%pizza%';

UPDATE products
SET category_id = 'f0000000-0000-0000-0000-000000000003'
WHERE (name ILIKE '%burger%' OR name ILIKE '%lavash%') AND category_id != 'f0000000-0000-0000-0000-000000000013';

-- 3.4 Bar (d0000000-0000-0000-0000-000000000004)
UPDATE categories
SET kitchen_id = 'd0000000-0000-0000-0000-000000000004',
    name = 'Ichimliklar & Choylar',
    name_uz = 'Ichimliklar & Choylar'
WHERE id = 'f0000000-0000-0000-0000-000000000004';

INSERT INTO categories (id, tenant_id, kitchen_id, name, name_uz, name_ru, icon, color, sort_order, is_active)
VALUES (
    'f0000000-0000-0000-0000-000000000014',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000004',
    'Sovuq ichimliklar',
    'Sovuq ichimliklar',
    'Холодные напитки',
    'cup',
    '#06b6d4',
    4,
    TRUE
) ON CONFLICT (id) DO UPDATE SET kitchen_id = EXCLUDED.kitchen_id;

INSERT INTO categories (id, tenant_id, kitchen_id, name, name_uz, name_ru, icon, color, sort_order, is_active)
VALUES (
    'f0000000-0000-0000-0000-000000000015',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000004',
    'Issiq ichimliklar',
    'Issiq ichimliklar',
    'Горячие напитки',
    'coffee',
    '#3b82f6',
    5,
    TRUE
) ON CONFLICT (id) DO UPDATE SET kitchen_id = EXCLUDED.kitchen_id;

UPDATE products
SET category_id = 'f0000000-0000-0000-0000-000000000014'
WHERE name ILIKE '%cola%' OR name ILIKE '%fanta%' OR name ILIKE '%suv%';

UPDATE products
SET category_id = 'f0000000-0000-0000-0000-000000000015'
WHERE name ILIKE '%choy%';

-- 3.5 Asosiy oshxona (d0000000-0000-0000-0000-000000000005)
UPDATE categories
SET kitchen_id = 'd0000000-0000-0000-0000-000000000005',
    name = 'Milliy taomlar',
    name_uz = 'Milliy taomlar'
WHERE id = 'f0000000-0000-0000-0000-000000000001';

UPDATE categories
SET kitchen_id = 'd0000000-0000-0000-0000-000000000005',
    name = 'Salatlar',
    name_uz = 'Salatlar'
WHERE id = 'f0000000-0000-0000-0000-000000000002';

INSERT INTO categories (id, tenant_id, kitchen_id, name, name_uz, name_ru, icon, color, sort_order, is_active)
VALUES (
    'f0000000-0000-0000-0000-000000000016',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000005',
    'Sho''rvalar',
    'Sho''rvalar',
    'Супы',
    'soup',
    '#10b981',
    6,
    TRUE
) ON CONFLICT (id) DO UPDATE SET kitchen_id = EXCLUDED.kitchen_id;

UPDATE products
SET category_id = 'f0000000-0000-0000-0000-000000000016'
WHERE name ILIKE '%sho%rva%';

UPDATE categories
SET kitchen_id = 'd0000000-0000-0000-0000-000000000005'
WHERE id IN ('f0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000006');

-- 4. Har qanday qolgan kategoriyalarni Asosiy oshxonaga biriktirish
UPDATE categories
SET kitchen_id = 'd0000000-0000-0000-0000-000000000005'
WHERE kitchen_id IS NULL;

-- 5. Barcha mahsulotlarning kitchen_id sini ularning yangi kategoriyasidagi kitchen_id ga sinxronlash
UPDATE products p
SET kitchen_id = c.kitchen_id
FROM categories c
WHERE p.category_id = c.id;

-- 6. Check constraint va Foreign keylarni o'rnatish
ALTER TABLE categories ALTER COLUMN kitchen_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_categories_kitchen'
    ) THEN
        ALTER TABLE categories 
        ADD CONSTRAINT fk_categories_kitchen 
        FOREIGN KEY (kitchen_id) REFERENCES kitchens(id) ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_products_category'
    ) THEN
        ALTER TABLE products 
        ADD CONSTRAINT fk_products_category 
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT;
    END IF;
END $$;
