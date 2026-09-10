-- ============================================================
-- V8: PRINTER MANAGEMENT AND PROFESSIONAL RESTAURANT SETTINGS
-- ============================================================

-- 1. Create printers table
CREATE TABLE IF NOT EXISTS printers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    model VARCHAR(100),
    connection_type VARCHAR(30) NOT NULL DEFAULT 'NETWORK', -- 'WINDOWS', 'USB', 'NETWORK', 'TCP/IP'
    ip_address VARCHAR(45),
    port INT DEFAULT 9100,
    windows_printer_name VARCHAR(255),
    paper_width INT NOT NULL DEFAULT 80, -- 58 or 80 mm
    character_encoding VARCHAR(50) NOT NULL DEFAULT 'UTF-8',
    purpose VARCHAR(30) NOT NULL DEFAULT 'KITCHEN', -- 'KITCHEN', 'CASHIER', 'RECEIPT', 'BAR', 'OTHER'
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE', -- 'ONLINE', 'OFFLINE', 'ERROR', 'UNKNOWN'
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    auto_print BOOLEAN NOT NULL DEFAULT TRUE,
    fallback_printer_id UUID REFERENCES printers(id) ON DELETE SET NULL,
    last_checked_at TIMESTAMPTZ,
    last_successful_print_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_printers_tenant ON printers(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_printers_purpose ON printers(tenant_id, purpose) WHERE deleted_at IS NULL;

-- 2. Create printer_assignments table
CREATE TABLE IF NOT EXISTS printer_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    printer_id UUID NOT NULL REFERENCES printers(id) ON DELETE CASCADE,
    kitchen_id UUID REFERENCES kitchens(id) ON DELETE CASCADE,
    purpose VARCHAR(30) NOT NULL DEFAULT 'KITCHEN',
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_printer_assignment UNIQUE (tenant_id, printer_id, kitchen_id)
);

CREATE INDEX IF NOT EXISTS idx_printer_assignments_kitchen ON printer_assignments(kitchen_id) WHERE is_active = TRUE AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_printer_assignments_printer ON printer_assignments(printer_id) WHERE is_active = TRUE AND deleted_at IS NULL;

-- 3. Extend kitchens table with visual and operational settings
ALTER TABLE kitchens ADD COLUMN IF NOT EXISTS color VARCHAR(30) DEFAULT '#6366F1';
ALTER TABLE kitchens ADD COLUMN IF NOT EXISTS auto_print BOOLEAN DEFAULT TRUE;
ALTER TABLE kitchens ADD COLUMN IF NOT EXISTS sound_notification BOOLEAN DEFAULT TRUE;
ALTER TABLE kitchens ADD COLUMN IF NOT EXISTS preparation_time_minutes INT DEFAULT 15;

-- Update existing kitchens with distinct modern colors
UPDATE kitchens SET color = '#F59E0B' WHERE code = 'PLOV';
UPDATE kitchens SET color = '#EC4899' WHERE code = 'SOMSA';
UPDATE kitchens SET color = '#EF4444' WHERE code = 'PIZZA';
UPDATE kitchens SET color = '#06B6D4' WHERE code = 'BAR';
UPDATE kitchens SET color = '#10B981' WHERE code = 'MAIN';

-- 4. Seed default printers for main tenant (a0000000-0000-0000-0000-000000000001)
INSERT INTO printers (id, tenant_id, name, model, connection_type, ip_address, port, windows_printer_name, paper_width, character_encoding, purpose, status, is_active, is_default, auto_print)
VALUES
    ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Printer-01 (Palovchi)', 'Bixolon SRP-350', 'NETWORK', '192.168.1.101', 9100, NULL, 80, 'UTF-8', 'KITCHEN', 'ONLINE', TRUE, FALSE, TRUE),
    ('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Printer-02 (Somsapaz)', 'EPSON TM-T20III', 'USB', NULL, NULL, 'EPSON TM-T20III Somsapaz', 80, 'UTF-8', 'KITCHEN', 'ONLINE', TRUE, FALSE, TRUE),
    ('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Printer-03 (Pitsaxona)', 'XPrinter XP-Q200', 'TCPIP', '192.168.1.105', 9100, NULL, 80, 'UTF-8', 'KITCHEN', 'ONLINE', TRUE, FALSE, TRUE),
    ('e0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Printer-04 (Bar)', 'POS-80 Network', 'NETWORK', '192.168.1.104', 9100, NULL, 80, 'UTF-8', 'KITCHEN', 'ONLINE', TRUE, FALSE, TRUE),
    ('e0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'Printer-05 (Asosiy Oshxona)', 'Sam4s Giant-100', 'NETWORK', '192.168.1.102', 9100, NULL, 80, 'UTF-8', 'KITCHEN', 'ONLINE', TRUE, FALSE, TRUE),
    ('e0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'Printer-Kassa (Receipt)', 'EPSON TM-T20III', 'WINDOWS', NULL, NULL, 'EPSON TM-T20III Receipt', 80, 'UTF-8', 'CASHIER', 'ONLINE', TRUE, TRUE, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 5. Set fallback printer: Pitsaxona printer fallback is Asosiy Oshxona printer
UPDATE printers SET fallback_printer_id = 'e0000000-0000-0000-0000-000000000005'
WHERE id = 'e0000000-0000-0000-0000-000000000003';

-- 6. Link printers to kitchens in printer_assignments
INSERT INTO printer_assignments (tenant_id, printer_id, kitchen_id, purpose, is_primary, is_active)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'KITCHEN', TRUE, TRUE), -- Palovchi
    ('a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'KITCHEN', TRUE, TRUE), -- Somsapaz
    ('a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'KITCHEN', TRUE, TRUE), -- Pitsaxona
    ('a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', 'KITCHEN', TRUE, TRUE), -- Bar
    ('a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000005', 'KITCHEN', TRUE, TRUE), -- Asosiy oshxona
    ('a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000006', NULL,                                 'CASHIER', TRUE, TRUE)  -- Cashier
ON CONFLICT DO NOTHING;

-- 7. Seed initial settings for all 16 categories
INSERT INTO settings (tenant_id, category, key, value, value_type, description, is_public)
VALUES
    -- GENERAL
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'defaultCurrency', 'UZS', 'string', 'Birlamchi valyuta', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'dateFormat', 'DD.MM.YYYY', 'string', 'Sana formati', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'timeFormat', 'HH:mm', 'string', 'Vaqt formati', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'language', 'uz', 'string', 'Tizim tili', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'theme', 'dark', 'string', 'Mavzu (dark/light)', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'autoSave', 'true', 'boolean', 'Avtomatik saqlash', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'autoRefresh', 'true', 'boolean', 'Avtomatik yangilash', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'realTimeUpdates', 'true', 'boolean', 'Real-time WebSocket ulanish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'confirmBeforeDelete', 'true', 'boolean', 'O''chirishdan oldin tasdiqlash', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'confirmBeforeCancelOrder', 'true', 'boolean', 'Buyurtmani bekor qilishdan oldin tasdiqlash', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'confirmBeforePayment', 'true', 'boolean', 'To''lovdan oldin tasdiqlash', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'GENERAL', 'soundNotifications', 'true', 'boolean', 'Ovozli bildirishnomalar', TRUE),

    -- RECEIPT
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'header', 'Xush kelibsiz! Marhamat, rohatlaning!', 'string', 'Chek yuqori yozuvi', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'footer', 'Tashrifingiz uchun rahmat! Yana kutib qolamiz!', 'string', 'Chek pastki yozuvi', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'showWaiter', 'true', 'boolean', 'Ofitsiant nomini ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'showCashier', 'true', 'boolean', 'Kassir nomini ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'showTable', 'true', 'boolean', 'Stol raqamini ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'showOrderNumber', 'true', 'boolean', 'Buyurtma raqamini ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'showDateTime', 'true', 'boolean', 'Sana va vaqtni ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'showPaymentMethod', 'true', 'boolean', 'To''lov usulini ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'showDiscount', 'true', 'boolean', 'Chegirmani ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'showServiceCharge', 'true', 'boolean', 'Xizmat haqini ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'showTax', 'false', 'boolean', 'Soliqni ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'paperWidth', '80', 'number', 'Chek qog''oz kengligi (mm)', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'RECEIPT', 'numberOfCopies', '1', 'number', 'Nusxalar soni', TRUE),

    -- PAYMENTS
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENTS', 'cashEnabled', 'true', 'boolean', 'Naqd to''lov faol', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENTS', 'cardEnabled', 'true', 'boolean', 'Bank kartasi to''lovi faol', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENTS', 'clickEnabled', 'true', 'boolean', 'Click to''lovi faol', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENTS', 'paymeEnabled', 'true', 'boolean', 'Payme to''lovi faol', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENTS', 'otherEnabled', 'false', 'boolean', 'Boshqa to''lov usullari', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENTS', 'defaultPaymentMethod', 'CASH', 'string', 'Birlamchi to''lov usuli', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENTS', 'allowMixedPayment', 'true', 'boolean', 'Aralash to''lovga ruxsat', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENTS', 'requirePaymentConfirmation', 'false', 'boolean', 'To''lov tasdig''i', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'PAYMENTS', 'autoPrintReceiptAfterPayment', 'true', 'boolean', 'To''lovdan so''ng avtomatik chek chiqarish', TRUE),

    -- TAX & SERVICE
    ('a0000000-0000-0000-0000-000000000001', 'TAX_SERVICE', 'serviceChargeEnabled', 'true', 'boolean', 'Xizmat haqi faol', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'TAX_SERVICE', 'serviceChargePercent', '10.0', 'number', 'Xizmat haqi foizi (%)', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'TAX_SERVICE', 'taxEnabled', 'false', 'boolean', 'Soliq (QQS) faol', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'TAX_SERVICE', 'taxPercent', '12.0', 'number', 'Soliq (QQS) foizi (%)', TRUE),

    -- ORDER SETTINGS
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'autoOrderNumber', 'true', 'boolean', 'Avtomatik buyurtma raqamlash', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'orderNumberPrefix', 'ORD', 'string', 'Buyurtma prefiksi', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'allowOrderEditing', 'true', 'boolean', 'Buyurtmani tahrirlashga ruxsat', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'allowItemCancellation', 'true', 'boolean', 'Taomni bekor qilishga ruxsat', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'allowQuantityEditing', 'true', 'boolean', 'Miqdorni o''zgartirishga ruxsat', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'requireCancellationReason', 'true', 'boolean', 'Bekor qilish sababi majburiyligi', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'requireManagerApproval', 'false', 'boolean', 'Menejer tasdig''i talab qilinishi', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'autoSendToKitchen', 'true', 'boolean', 'Oshxonaga avto yuborish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'allowSplitBill', 'true', 'boolean', 'Hisobni bo''lishga ruxsat', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'allowMergeOrders', 'true', 'boolean', 'Buyurtmalarni birlashtirish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'ORDERS', 'allowReopenOrder', 'false', 'boolean', 'Yopilgan buyurtmani qayta ochish', TRUE),

    -- KITCHEN SETTINGS
    ('a0000000-0000-0000-0000-000000000001', 'KITCHEN', 'autoPrintKitchenOrder', 'true', 'boolean', 'Oshxona buyurtmasini avto-chop etish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'KITCHEN', 'soundNotificationOnNewTicket', 'true', 'boolean', 'Yangi ticket kelganda ovoz berish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'KITCHEN', 'autoAcceptOrders', 'false', 'boolean', 'Ticketlarni avtomatik qabul qilish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'KITCHEN', 'ticketFontSize', 'medium', 'string', 'Ticket shrift hajmi (small/medium/large)', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'KITCHEN', 'showWaiterOnTicket', 'true', 'boolean', 'Ticketda ofitsiant nomini ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'KITCHEN', 'showTableOnTicket', 'true', 'boolean', 'Ticketda stol raqamini ko''rsatish', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'KITCHEN', 'showNotesOnTicket', 'true', 'boolean', 'Ticketda maxsus eslatmalarni ko''rsatish', TRUE),

    -- NOTIFICATIONS
    ('a0000000-0000-0000-0000-000000000001', 'NOTIFICATIONS', 'soundEnabled', 'true', 'boolean', 'Ovozlar yoqilgan', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'NOTIFICATIONS', 'soundVolume', '80', 'number', 'Ovoz balandligi (0-100)', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'NOTIFICATIONS', 'newOrderSound', 'chime', 'string', 'Yangi buyurtma ovozi', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'NOTIFICATIONS', 'itemReadySound', 'bell', 'string', 'Taom tayyor ovozi', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'NOTIFICATIONS', 'lowStockAlert', 'true', 'boolean', 'Kam qoldiq ogohlantirishi', TRUE),

    -- SECURITY
    ('a0000000-0000-0000-0000-000000000001', 'SECURITY', 'requirePinForCashier', 'true', 'boolean', 'Kassir uchun PIN so''rash', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'SECURITY', 'autoLogoutMinutes', '30', 'number', 'Avtomatik chiqib ketish (daqiqa)', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'SECURITY', 'sessionTimeoutMinutes', '120', 'number', 'Sessiya muddati (daqiqa)', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'SECURITY', 'maxLoginAttempts', '5', 'number', 'Maksimal noto''g''ri urinishlar', TRUE),

    -- BACKUP
    ('a0000000-0000-0000-0000-000000000001', 'BACKUP', 'backupLocation', 'C:/posOybek/backups', 'string', 'Zaxira papkasi manzili', FALSE),
    ('a0000000-0000-0000-0000-000000000001', 'BACKUP', 'autoBackupEnabled', 'true', 'boolean', 'Avtomatik kunlik zaxira', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'BACKUP', 'backupFrequency', 'DAILY', 'string', 'Zaxiralash chastotasi', TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'BACKUP', 'lastBackupTime', '2026-09-10 23:00:00', 'string', 'Oxirgi zaxira vaqti', TRUE)
ON CONFLICT (tenant_id, category, key) DO UPDATE SET
    value = EXCLUDED.value,
    description = EXCLUDED.description;
