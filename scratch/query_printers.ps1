$env:PGPASSWORD = '123'
$cmd = @"
SELECT id, name, purpose, windows_printer_name, status, is_active FROM printers WHERE deleted_at IS NULL;
"@
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U pos_user -h localhost -p 5433 -d pos -c "$cmd"
