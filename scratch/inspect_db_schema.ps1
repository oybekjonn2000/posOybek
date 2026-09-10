$env:PGPASSWORD = '123'
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -p 5433 -U pos_user -d pos -c "\d users"
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -p 5433 -U pos_user -d pos -c "SELECT username, active FROM users;"
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -p 5433 -U pos_user -d pos -c "SELECT id, name, code FROM kitchens;"
