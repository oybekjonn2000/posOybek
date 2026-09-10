$env:PGPASSWORD = '123'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psql -h localhost -p 5433 -U pos_user -d pos -c "
SELECT p.id, p.name, p.sale_price, c.name as category, k.name as kitchen
FROM products p
JOIN categories c ON p.category_id = c.id
JOIN kitchens k ON c.kitchen_id = k.id
WHERE p.is_active = TRUE
ORDER BY k.code, c.name, p.name;
"
& $psql -h localhost -p 5433 -U pos_user -d pos -c "
SELECT id, table_number, name, status, waiter_id, current_order_id, is_active FROM restaurant_tables WHERE is_active = TRUE ORDER BY table_number;
"
