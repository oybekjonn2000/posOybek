$env:PGPASSWORD = '123'
$cmd = @"
SELECT o.id, o.order_number, o.status, oi.product_name, oi.quantity, oi.kitchen_status, oi.is_voided
FROM orders o 
JOIN order_items oi ON o.id = oi.order_id 
WHERE o.id IN ('36d3da63-8292-4d1f-bf65-8a5580296a71', '32940d5f-6c80-4e80-92c9-e8c546248b8e', 'db350e55-01eb-4ec7-b93d-e32bab7becee')
ORDER BY o.id, oi.created_at;
"@
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U pos_user -h localhost -p 5433 -d pos -c "$cmd"
