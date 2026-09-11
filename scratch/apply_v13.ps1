$env:PGPASSWORD = '123'
$cmd = @"
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_kitchen_status_check;
ALTER TABLE order_items ADD CONSTRAINT order_items_kitchen_status_check 
    CHECK (kitchen_status IN (
        'NEW', 
        'PARTIALLY_SENT', 
        'SENT_TO_KITCHEN', 
        'ACCEPTED', 
        'PREPARING', 
        'COOKING', 
        'READY', 
        'DELIVERED', 
        'SERVED', 
        'CANCELLED'
    ));
"@
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U pos_user -h localhost -p 5433 -d pos -c "$cmd"
