$env:PGPASSWORD = '123'
$cmd = @"
SELECT order_id, product_id, count(*) FROM order_items WHERE is_voided = false GROUP BY order_id, product_id HAVING count(*) > 1;
"@
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U pos_user -h localhost -p 5433 -d pos -c "$cmd"
