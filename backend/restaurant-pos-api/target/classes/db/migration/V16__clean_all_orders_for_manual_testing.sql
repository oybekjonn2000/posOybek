-- ============================================================
-- V16__clean_all_orders_for_manual_testing.sql
-- Tizimni toza boshlang'ich holatga keltirish:
-- Barcha test buyurtmalari, cheklar, to'lovlar va biletlarni tozalash.
-- Foydalanuvchi o'zi qo'lda buyurtma yaratib sinovdan o'tkazishi uchun.
-- ============================================================

-- 1. Barcha stollarni FREE holatiga o'tkazish
UPDATE restaurant_tables 
SET status = 'FREE', current_order_id = NULL, waiter_id = NULL;

-- 2. Avvalgi test buyurtmalari va ularga bog'liq ma'lumotlarni tozalash
DELETE FROM order_item_modifiers;
DELETE FROM cancellation_receipts;
DELETE FROM kitchen_tickets;
DELETE FROM payments;
DELETE FROM order_items;
DELETE FROM orders;
