$env:PGPASSWORD = '123'
$cmd = @"
SELECT id, printer_id, kitchen_id, purpose, is_primary, is_active FROM printer_assignments WHERE deleted_at IS NULL;
"@
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U pos_user -h localhost -p 5433 -d pos -c "$cmd"
