-- ============================================================
-- V23__waiter_tables_only_permission.sql
-- WAITER rolidan faqat MANAGE_TABLES qolsin.
-- CREATE_ORDER, EDIT_ORDER va boshqa permissiyalar olinadi.
-- ============================================================

DELETE FROM role_permissions
WHERE role_id = 'b0000000-0000-0000-0000-000000000003'
  AND permission_id IN (
      SELECT id FROM permissions WHERE code IN (
          'CREATE_ORDER',
          'EDIT_ORDER',
          'DELETE_ORDER',
          'PROCESS_PAYMENT',
          'APPLY_DISCOUNT',
          'VIEW_REPORTS',
          'VIEW_DASHBOARD',
          'MANAGE_PRODUCTS',
          'MANAGE_CATEGORIES',
          'MANAGE_USERS',
          'MANAGE_SETTINGS',
          'MANAGE_DEVICES',
          'KITCHEN_VIEW',
          'KITCHEN_UPDATE',
          'REFUND',
          'EXPORT_REPORTS',
          'VIEW_SHIFTS',
          'MANAGE_SHIFTS',
          'MANAGE_CUSTOMERS',
          'MANAGE_WAREHOUSES',
          'MANAGE_STOCK',
          'VIEW_STOCK',
          'MANAGE_SUPPLIERS',
          'MANAGE_PURCHASES',
          'MANAGE_RECIPES',
          'BACKUP_RESTORE',
          'VIEW_AUDIT_LOGS',
          'MANAGE_ROLES'
      )
  );

-- WAITER uchun faqat MANAGE_TABLES qolsin
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'b0000000-0000-0000-0000-000000000003', id
FROM permissions
WHERE code = 'MANAGE_TABLES'
ON CONFLICT (role_id, permission_id) DO NOTHING;
