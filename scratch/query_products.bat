@set PGPASSWORD=123
@"C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -p 5433 -U pos_user -d pos -c "SELECT id, name, category_id, kitchen_id, is_active, is_available, deleted_at FROM products LIMIT 30;"
