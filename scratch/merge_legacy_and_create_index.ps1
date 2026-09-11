$env:PGPASSWORD = '123'
$cmd = @"
BEGIN;

-- 1. Merge duplicates in order db350e55-01eb-4ec7-b93d-e32bab7becee
UPDATE order_items 
SET quantity = 3.000, sent_quantity = 3.000, subtotal = 3.000 * unit_price
WHERE id = (SELECT id FROM order_items WHERE order_id = 'db350e55-01eb-4ec7-b93d-e32bab7becee' AND product_id = 'a1000000-0000-0000-0000-000000000031' ORDER BY created_at ASC LIMIT 1);

DELETE FROM order_items 
WHERE order_id = 'db350e55-01eb-4ec7-b93d-e32bab7becee' 
  AND product_id = 'a1000000-0000-0000-0000-000000000031' 
  AND id != (SELECT id FROM order_items WHERE order_id = 'db350e55-01eb-4ec7-b93d-e32bab7becee' AND product_id = 'a1000000-0000-0000-0000-000000000031' ORDER BY created_at ASC LIMIT 1);

-- 2. Merge duplicates in order 32940d5f-6c80-4e80-92c9-e8c546248b8e
UPDATE order_items 
SET quantity = 4.000, sent_quantity = 4.000, subtotal = 4.000 * unit_price
WHERE id = (SELECT id FROM order_items WHERE order_id = '32940d5f-6c80-4e80-92c9-e8c546248b8e' AND product_id = 'a1000000-0000-0000-0000-000000000031' ORDER BY created_at ASC LIMIT 1);

DELETE FROM order_items 
WHERE order_id = '32940d5f-6c80-4e80-92c9-e8c546248b8e' 
  AND product_id = 'a1000000-0000-0000-0000-000000000031' 
  AND id != (SELECT id FROM order_items WHERE order_id = '32940d5f-6c80-4e80-92c9-e8c546248b8e' AND product_id = 'a1000000-0000-0000-0000-000000000031' ORDER BY created_at ASC LIMIT 1);

-- 3. Merge duplicates in order 36d3da63-8292-4d1f-bf65-8a5580296a71
UPDATE order_items 
SET quantity = 2.000, sent_quantity = 2.000, subtotal = 2.000 * unit_price
WHERE id = (SELECT id FROM order_items WHERE order_id = '36d3da63-8292-4d1f-bf65-8a5580296a71' AND product_id = 'a1000000-0000-0000-0000-000000000031' ORDER BY created_at ASC LIMIT 1);

DELETE FROM order_items 
WHERE order_id = '36d3da63-8292-4d1f-bf65-8a5580296a71' 
  AND product_id = 'a1000000-0000-0000-0000-000000000031' 
  AND id != (SELECT id FROM order_items WHERE order_id = '36d3da63-8292-4d1f-bf65-8a5580296a71' AND product_id = 'a1000000-0000-0000-0000-000000000031' ORDER BY created_at ASC LIMIT 1);

-- 4. Create Partial Unique Index
CREATE UNIQUE INDEX IF NOT EXISTS uq_order_items_active_product 
    ON order_items (order_id, product_id) 
    WHERE is_voided = false;

COMMIT;
"@
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U pos_user -h localhost -p 5433 -d pos -c "$cmd"
