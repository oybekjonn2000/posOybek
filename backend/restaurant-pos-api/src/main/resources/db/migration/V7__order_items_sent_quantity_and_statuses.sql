-- V7: Order items sent quantity and extended kitchen statuses
-- Supports partial routing, incremental quantity additions, and duplicate prevention

-- 1. Add quantity tracking columns to order_items
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS sent_quantity NUMERIC(10,3) NOT NULL DEFAULT 0.000;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS delivered_quantity NUMERIC(10,3) NOT NULL DEFAULT 0.000;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS cancelled_quantity NUMERIC(10,3) NOT NULL DEFAULT 0.000;

-- 2. Update check constraint for kitchen_status to include SENT_TO_KITCHEN, PREPARING, DELIVERED
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_kitchen_status_check;
ALTER TABLE order_items ADD CONSTRAINT order_items_kitchen_status_check 
    CHECK (kitchen_status IN (
        'NEW', 
        'SENT_TO_KITCHEN', 
        'ACCEPTED', 
        'PREPARING', 
        'COOKING', 
        'READY', 
        'DELIVERED', 
        'SERVED', 
        'CANCELLED'
    ));

-- 3. Set sent_quantity = quantity for items already in kitchen processing or delivered
UPDATE order_items 
SET sent_quantity = quantity 
WHERE kitchen_status IN ('SENT_TO_KITCHEN', 'ACCEPTED', 'PREPARING', 'COOKING', 'READY', 'DELIVERED', 'SERVED')
  AND sent_quantity = 0;
