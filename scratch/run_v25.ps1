$env:PGPASSWORD = '123'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psql -h localhost -p 5433 -U pos_user -d pos -f "e:\posOybek\backend\restaurant-pos-api\src\main\resources\db\migration\V25__delivery_services_integration.sql"
