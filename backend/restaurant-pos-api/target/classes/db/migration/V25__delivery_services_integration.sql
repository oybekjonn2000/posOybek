-- ============================================================
-- V25__delivery_services_integration.sql
-- Yetkazib berish xizmatlari (Yandex, Uzum, Glovo, Custom API)
-- integratsiyasi uchun modellar, mappinglar, loglar va webhooklar
-- ============================================================

-- 1. DELIVERY PROVIDERS (Yetkazib berish xizmati provayderlari)
CREATE TABLE IF NOT EXISTS delivery_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    provider_type VARCHAR(50) NOT NULL CHECK (provider_type IN ('YANDEX', 'UZUM', 'GLOVO', 'CUSTOM')),
    status VARCHAR(30) NOT NULL DEFAULT 'DISCONNECTED' CHECK (status IN ('CONNECTED', 'DISCONNECTED', 'ERROR', 'DISABLED')),
    api_base_url VARCHAR(255),
    encrypted_api_key TEXT,
    encrypted_client_id TEXT,
    encrypted_secret TEXT,
    restaurant_id VARCHAR(100),
    webhook_url VARCHAR(255),
    webhook_secret TEXT,
    auto_accept BOOLEAN NOT NULL DEFAULT FALSE,
    auto_print_kitchen BOOLEAN NOT NULL DEFAULT TRUE,
    auto_print_receipt BOOLEAN NOT NULL DEFAULT FALSE,
    sound_notification BOOLEAN NOT NULL DEFAULT TRUE,
    auto_sync BOOLEAN NOT NULL DEFAULT TRUE,
    sync_interval_seconds INT NOT NULL DEFAULT 60,
    commission_type VARCHAR(30) NOT NULL DEFAULT 'PERCENTAGE' CHECK (commission_type IN ('PERCENTAGE', 'FIXED', 'PROVIDER_REPORTED')),
    commission_value NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    default_order_source VARCHAR(50) NOT NULL DEFAULT 'DELIVERY',
    default_payment_type VARCHAR(50) NOT NULL DEFAULT 'ONLINE',
    last_sync_at TIMESTAMPTZ,
    last_connection_test_at TIMESTAMPTZ,
    last_connection_status VARCHAR(30),
    last_connection_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_delivery_providers_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_delivery_providers_tenant ON delivery_providers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_delivery_providers_status ON delivery_providers(status);

-- 2. DELIVERY ORDERS (Yetkazib berish buyurtmalari)
CREATE TABLE IF NOT EXISTS delivery_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES delivery_providers(id) ON DELETE CASCADE,
    external_order_id VARCHAR(100) NOT NULL,
    pos_order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW' CHECK (
        status IN ('NEW', 'ACCEPTED', 'REJECTED', 'PREPARING', 'READY', 'COURIER_ASSIGNED', 'PICKED_UP', 'DELIVERING', 'DELIVERED', 'CANCELLED', 'FAILED', 'MAPPING_REQUIRED')
    ),
    customer_name VARCHAR(150),
    customer_phone VARCHAR(50),
    delivery_address TEXT,
    address_apartment VARCHAR(50),
    address_entrance VARCHAR(50),
    address_floor VARCHAR(50),
    address_comment TEXT,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    delivery_fee NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    commission NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    service_fee NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    payment_type VARCHAR(50) NOT NULL DEFAULT 'ONLINE' CHECK (payment_type IN ('ONLINE', 'CASH', 'CARD', 'UNKNOWN')),
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PAID', 'PENDING', 'FAILED')),
    courier_name VARCHAR(100),
    courier_phone VARCHAR(50),
    courier_id VARCHAR(100),
    courier_vehicle VARCHAR(50),
    courier_status VARCHAR(50),
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    raw_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_delivery_orders_provider_external UNIQUE (provider_id, external_order_id)
);

CREATE INDEX IF NOT EXISTS idx_delivery_orders_tenant_status ON delivery_orders(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_delivery_orders_pos_order ON delivery_orders(pos_order_id);
CREATE INDEX IF NOT EXISTS idx_delivery_orders_created_at ON delivery_orders(created_at DESC);

-- 3. DELIVERY ORDER ITEMS (Buyurtmadagi mahsulotlar)
CREATE TABLE IF NOT EXISTS delivery_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_order_id UUID NOT NULL REFERENCES delivery_orders(id) ON DELETE CASCADE,
    external_product_id VARCHAR(100),
    pos_product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    quantity NUMERIC(10, 3) NOT NULL DEFAULT 1.000,
    unit_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    mapping_status VARCHAR(30) NOT NULL DEFAULT 'MAPPED' CHECK (mapping_status IN ('MAPPED', 'UNMAPPED')),
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_delivery_order_items_order ON delivery_order_items(delivery_order_id);

-- 4. DELIVERY PRODUCT MAPPINGS (Tashqi mahsulot -> POS mahsulot bog'lanishi)
CREATE TABLE IF NOT EXISTS delivery_product_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES delivery_providers(id) ON DELETE CASCADE,
    external_product_id VARCHAR(100) NOT NULL,
    external_product_name VARCHAR(255),
    pos_product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    auto_mapped BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_delivery_product_mappings UNIQUE (provider_id, external_product_id)
);

CREATE INDEX IF NOT EXISTS idx_delivery_prod_mappings_tenant ON delivery_product_mappings(tenant_id, provider_id);

-- 5. DELIVERY CATEGORY MAPPINGS (Tashqi kategoriya -> POS kategoriya bog'lanishi)
CREATE TABLE IF NOT EXISTS delivery_category_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES delivery_providers(id) ON DELETE CASCADE,
    external_category_id VARCHAR(100) NOT NULL,
    external_category_name VARCHAR(255),
    pos_category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_delivery_category_mappings UNIQUE (provider_id, external_category_id)
);

CREATE INDEX IF NOT EXISTS idx_delivery_cat_mappings_tenant ON delivery_category_mappings(tenant_id, provider_id);

-- 6. DELIVERY WEBHOOK EVENTS (Webhook voqealarini qayd qilish va dublikatdan himoya)
CREATE TABLE IF NOT EXISTS delivery_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES delivery_providers(id) ON DELETE CASCADE,
    external_event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    payload TEXT,
    CONSTRAINT uq_delivery_webhook_events UNIQUE (provider_id, external_event_id)
);

CREATE INDEX IF NOT EXISTS idx_delivery_webhook_events_tenant ON delivery_webhook_events(tenant_id, provider_id);

-- 7. DELIVERY INTEGRATION LOGS (Integratsiya xavfsiz audit loglari)
CREATE TABLE IF NOT EXISTS delivery_integration_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    provider_id UUID REFERENCES delivery_providers(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    external_id VARCHAR(100),
    status VARCHAR(30) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING', 'WARNING')),
    request_time_ms BIGINT,
    error_message TEXT,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_delivery_logs_tenant_created ON delivery_integration_logs(tenant_id, created_at DESC);

-- Birlamchi 4 ta yetkazib berish xizmatini har bir tenant uchun seed qilish (DISCONNECTED holatda)
INSERT INTO delivery_providers (id, tenant_id, name, code, provider_type, status, commission_type, commission_value)
SELECT 
    gen_random_uuid(), 
    t.id, 
    'Yandex Eats', 
    'YANDEX_EATS', 
    'YANDEX', 
    'DISCONNECTED', 
    'PERCENTAGE', 
    15.00
FROM tenants t
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO delivery_providers (id, tenant_id, name, code, provider_type, status, commission_type, commission_value)
SELECT 
    gen_random_uuid(), 
    t.id, 
    'Uzum Tezkor', 
    'UZUM_TEZKOR', 
    'UZUM', 
    'DISCONNECTED', 
    'PERCENTAGE', 
    12.00
FROM tenants t
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO delivery_providers (id, tenant_id, name, code, provider_type, status, commission_type, commission_value)
SELECT 
    gen_random_uuid(), 
    t.id, 
    'Glovo', 
    'GLOVO', 
    'GLOVO', 
    'DISCONNECTED', 
    'PERCENTAGE', 
    18.00
FROM tenants t
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO delivery_providers (id, tenant_id, name, code, provider_type, status, commission_type, commission_value)
SELECT 
    gen_random_uuid(), 
    t.id, 
    'Custom API (O''z deliverymiz)', 
    'CUSTOM_API', 
    'CUSTOM', 
    'DISCONNECTED', 
    'FIXED', 
    5000.00
FROM tenants t
ON CONFLICT (tenant_id, code) DO NOTHING;
