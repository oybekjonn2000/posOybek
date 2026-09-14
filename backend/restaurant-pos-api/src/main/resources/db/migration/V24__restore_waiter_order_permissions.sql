-- ============================================================
-- V24__restore_waiter_order_permissions.sql
-- Ofitsiant stol ochib buyurtma berishi (zakas qilishi) uchun
-- CREATE_ORDER va EDIT_ORDER ruxsatlarini qaytarish
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'b0000000-0000-0000-0000-000000000003', id 
FROM permissions 
WHERE code IN ('CREATE_ORDER', 'EDIT_ORDER')
ON CONFLICT (role_id, permission_id) DO NOTHING;
