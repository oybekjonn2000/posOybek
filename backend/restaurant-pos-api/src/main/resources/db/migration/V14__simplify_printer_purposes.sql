-- V14: Simplify printer purposes to KITCHEN and CASHIER
-- Migrate any legacy RECEIPT, BAR, OTHER records to CASHIER
UPDATE printers 
SET purpose = 'CASHIER' 
WHERE purpose IN ('RECEIPT', 'OTHER', 'BAR');

UPDATE printer_assignments 
SET purpose = 'CASHIER' 
WHERE purpose IN ('RECEIPT', 'OTHER', 'BAR');
