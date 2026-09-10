$env:PGPASSWORD = '123'
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -p 5433 -U pos_user -d pos -c "SELECT code, name FROM permissions ORDER BY code;"
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -p 5433 -U pos_user -d pos -c "SELECT r.name, p.code FROM roles r JOIN role_permissions rp ON r.id = rp.role_id JOIN permissions p ON rp.permission_id = p.id WHERE r.name IN ('WAITER', 'KITCHEN');"
