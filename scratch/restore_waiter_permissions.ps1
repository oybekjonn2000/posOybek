$env:PGPASSWORD = '123'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psql -h localhost -p 5433 -U pos_user -d pos -c "
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'b0000000-0000-0000-0000-000000000003', id 
FROM permissions 
WHERE code IN ('CREATE_ORDER', 'EDIT_ORDER')
ON CONFLICT (role_id, permission_id) DO NOTHING;

SELECT r.name as role, p.code as permission_code, p.name as permission_name 
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.name = 'WAITER'
ORDER BY p.code;
"
