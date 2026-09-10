$env:PGPASSWORD = '123'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psql -h localhost -p 5433 -U pos_user -d pos -c "
UPDATE restaurant_tables SET status = 'FREE', current_order_id = NULL, waiter_id = NULL;
DELETE FROM kitchen_tickets;
DELETE FROM order_item_modifiers;
DELETE FROM order_items;
DELETE FROM payments;
DELETE FROM cancellation_receipts;
DELETE FROM orders;
SELECT id, table_number, name, status, waiter_id, current_order_id FROM restaurant_tables WHERE is_active = TRUE ORDER BY table_number;
"
