-- V12: Add receipt print status and tracking to orders
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS receipt_print_status VARCHAR(30) NOT NULL DEFAULT 'NOT_PRINTED',
ADD COLUMN IF NOT EXISTS receipt_printed_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS receipt_print_attempts INT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS receipt_print_error TEXT;

CREATE INDEX IF NOT EXISTS idx_orders_receipt_print_status ON orders(receipt_print_status);
