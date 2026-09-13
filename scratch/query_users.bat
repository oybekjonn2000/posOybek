@set PGPASSWORD=123
@"C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -p 5433 -U pos_user -d pos -c "SELECT id, username, first_name, last_name, is_active FROM users;"
