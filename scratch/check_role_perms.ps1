$env:PGPASSWORD = '123'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psql -h localhost -p 5433 -U pos_user -d pos -c "
SELECT r.name as role, p.name as permission 
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.name IN ('WAITER', 'KITCHEN')
ORDER BY r.name, p.name;
"
