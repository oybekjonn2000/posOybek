-- V13: Add PARTIALLY_SENT to order_items kitchen_status check constraint and unique index
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_kitchen_status_check;
ALTER TABLE order_items ADD CONSTRAINT order_items_kitchen_status_check 
    CHECK (kitchen_status IN (
        'NEW', 
        'PARTIALLY_SENT', 
        'SENT_TO_KITCHEN', 
        'ACCEPTED', 
        'PREPARING', 
        'COOKING', 
        'READY', 
        'DELIVERED', 
        'SERVED', 
        'CANCELLED'
    ));

CREATE UNIQUE INDEX IF NOT EXISTS uq_order_items_active_product 
    ON order_items (order_id, product_id) 
    WHERE is_voided = false;

