-- ============================================================
-- V1: Initial Schema - Restaurant POS Complete Database
-- Author: RestaurantPOS Team
-- Description: Full database schema with all core tables
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- TENANTS & DEVICES
-- ============================================================

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100) DEFAULT 'UZ',
    currency VARCHAR(10) NOT NULL DEFAULT 'UZS',
    timezone VARCHAR(100) NOT NULL DEFAULT 'Asia/Tashkent',
    logo_url TEXT,
    tax_number VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_tenants_slug ON tenants(slug) WHERE deleted_at IS NULL;

-- ============================================================
-- ROLES & PERMISSIONS
-- ============================================================

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(tenant_id, name)
);

CREATE INDEX idx_roles_tenant ON roles(tenant_id) WHERE deleted_at IS NULL;

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    module VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================
-- USERS & EMPLOYEES
-- ============================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    pin_hash VARCHAR(255),
    language VARCHAR(10) DEFAULT 'uz',
    theme VARCHAR(20) DEFAULT 'dark',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    UNIQUE(tenant_id, username)
);

CREATE INDEX idx_users_tenant ON users(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_username ON users(tenant_id, username) WHERE deleted_at IS NULL;

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    device_id UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ,
    ip_address VARCHAR(45),
    user_agent TEXT
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ============================================================
-- DEVICES
-- ============================================================

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    device_code VARCHAR(50) NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    device_type VARCHAR(50) NOT NULL CHECK (device_type IN ('POS','KITCHEN','MANAGER','SERVER','WAITER')),
    token VARCHAR(500),
    token_expires_at TIMESTAMPTZ,
    ip_address VARCHAR(45),
    mac_address VARCHAR(20),
    os_info VARCHAR(255),
    app_version VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_master BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at TIMESTAMPTZ,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(tenant_id, device_code)
);

CREATE INDEX idx_devices_tenant ON devices(tenant_id) WHERE deleted_at IS NULL;

-- ============================================================
-- CATEGORIES
-- ============================================================

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    name_uz VARCHAR(255),
    name_ru VARCHAR(255),
    name_en VARCHAR(255),
    description TEXT,
    icon VARCHAR(100),
    color VARCHAR(20),
    image_url TEXT,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    parent_id UUID REFERENCES categories(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

CREATE INDEX idx_categories_tenant ON categories(tenant_id) WHERE deleted_at IS NULL;

-- ============================================================
-- MODIFIER GROUPS & MODIFIERS
-- ============================================================

CREATE TABLE modifier_groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    min_selections INTEGER DEFAULT 0,
    max_selections INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE modifiers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    group_id UUID NOT NULL REFERENCES modifier_groups(id),
    name VARCHAR(255) NOT NULL,
    price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_modifiers_group ON modifiers(group_id) WHERE deleted_at IS NULL;

-- ============================================================
-- PRODUCTS
-- ============================================================

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    category_id UUID REFERENCES categories(id),
    sku VARCHAR(100),
    barcode VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    name_uz VARCHAR(255),
    name_ru VARCHAR(255),
    name_en VARCHAR(255),
    description TEXT,
    image_url TEXT,
    unit VARCHAR(50) NOT NULL DEFAULT 'piece',
    purchase_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    sale_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    tax_rate NUMERIC(5,2) DEFAULT 0.00,
    is_taxable BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    track_stock BOOLEAN NOT NULL DEFAULT TRUE,
    min_stock_level NUMERIC(15,3) DEFAULT 0.000,
    current_stock NUMERIC(15,3) DEFAULT 0.000,
    sort_order INTEGER DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

CREATE UNIQUE INDEX uq_products_tenant_sku ON products(tenant_id, sku) WHERE sku IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_products_tenant_barcode ON products(tenant_id, barcode) WHERE barcode IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_products_tenant ON products(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_category ON products(category_id) WHERE deleted_at IS NULL;

-- Product modifier group linking
CREATE TABLE product_modifier_groups (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
    sort_order INTEGER DEFAULT 0,
    PRIMARY KEY (product_id, modifier_group_id)
);

-- ============================================================
-- TABLE ZONES & TABLES
-- ============================================================

CREATE TABLE table_zones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE restaurant_tables (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    zone_id UUID REFERENCES table_zones(id),
    table_number VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    capacity INTEGER NOT NULL DEFAULT 4,
    shape VARCHAR(20) DEFAULT 'rectangle',
    pos_x INTEGER DEFAULT 0,
    pos_y INTEGER DEFAULT 0,
    width INTEGER DEFAULT 100,
    height INTEGER DEFAULT 80,
    status VARCHAR(30) NOT NULL DEFAULT 'FREE'
        CHECK (status IN ('FREE','OCCUPIED','RESERVED','BILL_REQUESTED','CLEANING')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    current_order_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

CREATE UNIQUE INDEX uq_tables_tenant_number ON restaurant_tables(tenant_id, table_number) WHERE deleted_at IS NULL;
CREATE INDEX idx_tables_tenant ON restaurant_tables(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tables_status ON restaurant_tables(tenant_id, status) WHERE deleted_at IS NULL;

-- ============================================================
-- CUSTOMERS
-- ============================================================

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    notes TEXT,
    total_orders INTEGER DEFAULT 0,
    total_spent NUMERIC(15,2) DEFAULT 0.00,
    last_order_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

CREATE INDEX idx_customers_tenant ON customers(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_phone ON customers(tenant_id, phone) WHERE deleted_at IS NULL;

-- ============================================================
-- SHIFTS
-- ============================================================

CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    device_id UUID REFERENCES devices(id),
    cashier_id UUID NOT NULL REFERENCES users(id),
    shift_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED')),
    opening_cash NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    closing_cash_expected NUMERIC(15,2) DEFAULT 0.00,
    closing_cash_actual NUMERIC(15,2),
    cash_difference NUMERIC(15,2),
    total_sales NUMERIC(15,2) DEFAULT 0.00,
    total_cash_sales NUMERIC(15,2) DEFAULT 0.00,
    total_card_sales NUMERIC(15,2) DEFAULT 0.00,
    total_refunds NUMERIC(15,2) DEFAULT 0.00,
    total_discounts NUMERIC(15,2) DEFAULT 0.00,
    orders_count INTEGER DEFAULT 0,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shifts_tenant ON shifts(tenant_id);
CREATE INDEX idx_shifts_cashier ON shifts(cashier_id);
CREATE INDEX idx_shifts_status ON shifts(tenant_id, status);

-- ============================================================
-- ORDERS
-- ============================================================

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    device_id UUID REFERENCES devices(id),
    shift_id UUID REFERENCES shifts(id),
    table_id UUID REFERENCES restaurant_tables(id),
    customer_id UUID REFERENCES customers(id),
    waiter_id UUID REFERENCES users(id),
    cashier_id UUID REFERENCES users(id),

    order_number VARCHAR(50) NOT NULL,
    order_type VARCHAR(20) NOT NULL DEFAULT 'DINE_IN'
        CHECK (order_type IN ('DINE_IN','TAKEAWAY','DELIVERY')),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN','IN_PROGRESS','READY','PAID','CANCELLED','REFUNDED')),

    subtotal NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    discount_percent NUMERIC(5,2) DEFAULT 0.00,
    tax_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total NUMERIC(15,2) NOT NULL DEFAULT 0.00,

    -- Delivery fields
    delivery_address TEXT,
    delivery_phone VARCHAR(50),
    delivery_notes TEXT,
    delivery_fee NUMERIC(15,2) DEFAULT 0.00,
    courier_id UUID REFERENCES users(id),

    notes TEXT,
    kitchen_notes TEXT,

    -- Timing
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_to_kitchen_at TIMESTAMPTZ,
    ready_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,

    -- Sync
    version INTEGER NOT NULL DEFAULT 1,
    local_sequence BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

CREATE INDEX idx_orders_tenant ON orders(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_status ON orders(tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_table ON orders(table_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_shift ON orders(shift_id);
CREATE INDEX idx_orders_number ON orders(tenant_id, order_number);
CREATE INDEX idx_orders_opened_at ON orders(tenant_id, opened_at);

-- ============================================================
-- ORDER ITEMS
-- ============================================================

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100),

    quantity NUMERIC(10,3) NOT NULL DEFAULT 1.000,
    unit_price NUMERIC(15,2) NOT NULL,
    discount_amount NUMERIC(15,2) DEFAULT 0.00,
    discount_percent NUMERIC(5,2) DEFAULT 0.00,
    tax_amount NUMERIC(15,2) DEFAULT 0.00,
    subtotal NUMERIC(15,2) NOT NULL,

    notes TEXT,
    kitchen_status VARCHAR(20) DEFAULT 'NEW'
        CHECK (kitchen_status IN ('NEW','ACCEPTED','COOKING','READY','SERVED','CANCELLED')),

    is_voided BOOLEAN DEFAULT FALSE,
    voided_at TIMESTAMPTZ,
    voided_by UUID REFERENCES users(id),
    void_reason TEXT,

    sort_order INTEGER DEFAULT 0,
    sent_to_kitchen_at TIMESTAMPTZ,
    ready_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
CREATE INDEX idx_order_items_kitchen_status ON order_items(kitchen_status);

-- ============================================================
-- ORDER ITEM MODIFIERS
-- ============================================================

CREATE TABLE order_item_modifiers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    modifier_id UUID NOT NULL REFERENCES modifiers(id),
    modifier_name VARCHAR(255) NOT NULL,
    price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_item_modifiers_item ON order_item_modifiers(order_item_id);

-- ============================================================
-- PAYMENTS
-- ============================================================

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    shift_id UUID REFERENCES shifts(id),
    cashier_id UUID REFERENCES users(id),
    device_id UUID REFERENCES devices(id),

    payment_number VARCHAR(50) NOT NULL,
    payment_method VARCHAR(30) NOT NULL
        CHECK (payment_method IN ('CASH','CARD','OTHER','MIXED')),
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
        CHECK (status IN ('PENDING','COMPLETED','REFUNDED','FAILED')),

    amount NUMERIC(15,2) NOT NULL,
    cash_amount NUMERIC(15,2) DEFAULT 0.00,
    card_amount NUMERIC(15,2) DEFAULT 0.00,
    other_amount NUMERIC(15,2) DEFAULT 0.00,
    change_amount NUMERIC(15,2) DEFAULT 0.00,

    is_refund BOOLEAN NOT NULL DEFAULT FALSE,
    original_payment_id UUID REFERENCES payments(id),
    refund_reason TEXT,

    reference_number VARCHAR(255),
    notes TEXT,

    paid_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_tenant ON payments(tenant_id);
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_shift ON payments(shift_id);
CREATE INDEX idx_payments_paid_at ON payments(tenant_id, paid_at);

-- ============================================================
-- CASH TRANSACTIONS (Shift cash movements)
-- ============================================================

CREATE TABLE cash_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    shift_id UUID NOT NULL REFERENCES shifts(id),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('CASH_IN','CASH_OUT','OPENING','CLOSING')),
    amount NUMERIC(15,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cash_transactions_shift ON cash_transactions(shift_id);

-- ============================================================
-- WAREHOUSES
-- ============================================================

CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_warehouses_tenant ON warehouses(tenant_id) WHERE deleted_at IS NULL;

-- ============================================================
-- INGREDIENTS / RAW MATERIALS
-- ============================================================

CREATE TABLE ingredients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(50) NOT NULL DEFAULT 'g',
    purchase_price NUMERIC(15,2) DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_ingredients_tenant ON ingredients(tenant_id) WHERE deleted_at IS NULL;

-- ============================================================
-- STOCK ITEMS (Warehouse stock levels)
-- ============================================================

CREATE TABLE stock_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    ingredient_id UUID REFERENCES ingredients(id),
    product_id UUID REFERENCES products(id),
    quantity NUMERIC(15,3) NOT NULL DEFAULT 0.000,
    unit VARCHAR(50) NOT NULL DEFAULT 'piece',
    min_quantity NUMERIC(15,3) DEFAULT 0.000,
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT stock_item_reference CHECK (
        (ingredient_id IS NOT NULL AND product_id IS NULL) OR
        (ingredient_id IS NULL AND product_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_stock_items_ingredient ON stock_items(warehouse_id, ingredient_id) WHERE ingredient_id IS NOT NULL;
CREATE UNIQUE INDEX uq_stock_items_product ON stock_items(warehouse_id, product_id) WHERE product_id IS NOT NULL;

CREATE INDEX idx_stock_items_warehouse ON stock_items(warehouse_id);
CREATE INDEX idx_stock_items_tenant ON stock_items(tenant_id);

-- ============================================================
-- STOCK MOVEMENTS (Immutable ledger)
-- ============================================================

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    warehouse_id UUID REFERENCES warehouses(id),
    from_warehouse_id UUID REFERENCES warehouses(id),
    to_warehouse_id UUID REFERENCES warehouses(id),
    ingredient_id UUID REFERENCES ingredients(id),
    product_id UUID REFERENCES products(id),

    movement_type VARCHAR(30) NOT NULL
        CHECK (movement_type IN ('PURCHASE','SALE','RETURN','ADJUSTMENT','WASTE','TRANSFER','OPENING')),

    quantity NUMERIC(15,3) NOT NULL,
    unit_cost NUMERIC(15,2),
    total_cost NUMERIC(15,2),

    reference_id UUID,  -- order_id or purchase_id
    reference_type VARCHAR(50),

    notes TEXT,
    created_by UUID REFERENCES users(id),
    device_id UUID REFERENCES devices(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_tenant ON stock_movements(tenant_id);
CREATE INDEX idx_stock_movements_warehouse ON stock_movements(warehouse_id);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(tenant_id, created_at);

-- ============================================================
-- RECIPES
-- ============================================================

CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL REFERENCES products(id),
    warehouse_id UUID REFERENCES warehouses(id),
    yield_quantity NUMERIC(10,3) NOT NULL DEFAULT 1.000,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_recipes_product ON recipes(product_id) WHERE deleted_at IS NULL;

CREATE TABLE recipe_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id UUID NOT NULL REFERENCES ingredients(id),
    quantity NUMERIC(15,3) NOT NULL,
    unit VARCHAR(50) NOT NULL DEFAULT 'g',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recipe_items_recipe ON recipe_items(recipe_id);

-- ============================================================
-- SUPPLIERS
-- ============================================================

CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    contact_person VARCHAR(255),
    tax_number VARCHAR(100),
    total_debt NUMERIC(15,2) DEFAULT 0.00,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_suppliers_tenant ON suppliers(tenant_id) WHERE deleted_at IS NULL;

-- ============================================================
-- PURCHASES
-- ============================================================

CREATE TABLE purchases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    supplier_id UUID REFERENCES suppliers(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    user_id UUID NOT NULL REFERENCES users(id),
    device_id UUID REFERENCES devices(id),

    purchase_number VARCHAR(50) NOT NULL,
    invoice_number VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT','PENDING','CONFIRMED','CANCELLED')),

    subtotal NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15,2) DEFAULT 0.00,
    tax_amount NUMERIC(15,2) DEFAULT 0.00,
    total NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    paid_amount NUMERIC(15,2) DEFAULT 0.00,
    debt_amount NUMERIC(15,2) DEFAULT 0.00,

    notes TEXT,
    purchase_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_purchases_tenant ON purchases(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchases_supplier ON purchases(supplier_id);

CREATE TABLE purchase_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    purchase_id UUID NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    ingredient_id UUID REFERENCES ingredients(id),
    product_id UUID REFERENCES products(id),
    item_name VARCHAR(255) NOT NULL,
    quantity NUMERIC(15,3) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL,
    total NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_purchase_items_purchase ON purchase_items(purchase_id);

-- ============================================================
-- SYNC EVENTS (Offline-first synchronization)
-- ============================================================

CREATE TABLE sync_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    device_id UUID REFERENCES devices(id),
    user_id UUID REFERENCES users(id),

    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL CHECK (operation IN ('CREATE','UPDATE','DELETE')),
    payload JSONB NOT NULL,

    version BIGINT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','SYNCING','SYNCED','FAILED')),
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    synced_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ
);

CREATE INDEX idx_sync_events_tenant_status ON sync_events(tenant_id, status);
CREATE INDEX idx_sync_events_device ON sync_events(device_id, status);
CREATE INDEX idx_sync_events_created_at ON sync_events(created_at);
CREATE INDEX idx_sync_events_next_retry ON sync_events(next_retry_at) WHERE status = 'FAILED';

-- ============================================================
-- AUDIT LOGS
-- ============================================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID REFERENCES users(id),
    device_id UUID REFERENCES devices(id),

    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,

    old_value JSONB,
    new_value JSONB,

    ip_address VARCHAR(45),
    user_agent TEXT,
    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(tenant_id, created_at);

-- ============================================================
-- SETTINGS
-- ============================================================

CREATE TABLE settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    category VARCHAR(100) NOT NULL,
    key VARCHAR(200) NOT NULL,
    value TEXT,
    value_type VARCHAR(20) DEFAULT 'string',
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, category, key)
);

CREATE INDEX idx_settings_tenant ON settings(tenant_id);
CREATE INDEX idx_settings_category ON settings(tenant_id, category);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID REFERENCES users(id),
    device_id UUID REFERENCES devices(id),

    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    data JSONB,

    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_tenant ON notifications(tenant_id, created_at);

-- ============================================================
-- DEVICE SEQUENCE (for unique order numbers per device)
-- ============================================================

CREATE TABLE device_sequences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    sequence_type VARCHAR(50) NOT NULL DEFAULT 'ORDER',
    current_value BIGINT NOT NULL DEFAULT 0,
    UNIQUE(device_id, sequence_type)
);

-- ============================================================
-- INITIAL PERMISSIONS DATA
-- ============================================================

INSERT INTO permissions (id, code, name, module) VALUES
    (uuid_generate_v4(), 'VIEW_DASHBOARD', 'View Dashboard', 'dashboard'),
    (uuid_generate_v4(), 'CREATE_ORDER', 'Create Order', 'orders'),
    (uuid_generate_v4(), 'EDIT_ORDER', 'Edit Order', 'orders'),
    (uuid_generate_v4(), 'DELETE_ORDER', 'Delete Order', 'orders'),
    (uuid_generate_v4(), 'APPLY_DISCOUNT', 'Apply Discount', 'orders'),
    (uuid_generate_v4(), 'PROCESS_PAYMENT', 'Process Payment', 'payments'),
    (uuid_generate_v4(), 'REFUND', 'Process Refund', 'payments'),
    (uuid_generate_v4(), 'VIEW_REPORTS', 'View Reports', 'reports'),
    (uuid_generate_v4(), 'EXPORT_REPORTS', 'Export Reports', 'reports'),
    (uuid_generate_v4(), 'MANAGE_PRODUCTS', 'Manage Products', 'products'),
    (uuid_generate_v4(), 'MANAGE_CATEGORIES', 'Manage Categories', 'products'),
    (uuid_generate_v4(), 'MANAGE_STOCK', 'Manage Stock', 'inventory'),
    (uuid_generate_v4(), 'VIEW_STOCK', 'View Stock', 'inventory'),
    (uuid_generate_v4(), 'MANAGE_PURCHASES', 'Manage Purchases', 'inventory'),
    (uuid_generate_v4(), 'MANAGE_SUPPLIERS', 'Manage Suppliers', 'inventory'),
    (uuid_generate_v4(), 'MANAGE_RECIPES', 'Manage Recipes', 'inventory'),
    (uuid_generate_v4(), 'MANAGE_USERS', 'Manage Users', 'users'),
    (uuid_generate_v4(), 'MANAGE_ROLES', 'Manage Roles', 'users'),
    (uuid_generate_v4(), 'MANAGE_TABLES', 'Manage Tables', 'tables'),
    (uuid_generate_v4(), 'MANAGE_CUSTOMERS', 'Manage Customers', 'customers'),
    (uuid_generate_v4(), 'MANAGE_SETTINGS', 'Manage Settings', 'settings'),
    (uuid_generate_v4(), 'MANAGE_DEVICES', 'Manage Devices', 'devices'),
    (uuid_generate_v4(), 'MANAGE_SHIFTS', 'Manage Shifts', 'shifts'),
    (uuid_generate_v4(), 'VIEW_SHIFTS', 'View Shifts', 'shifts'),
    (uuid_generate_v4(), 'BACKUP_RESTORE', 'Backup & Restore', 'system'),
    (uuid_generate_v4(), 'VIEW_AUDIT_LOGS', 'View Audit Logs', 'system'),
    (uuid_generate_v4(), 'MANAGE_WAREHOUSES', 'Manage Warehouses', 'inventory'),
    (uuid_generate_v4(), 'KITCHEN_VIEW', 'Kitchen Display View', 'kitchen'),
    (uuid_generate_v4(), 'KITCHEN_UPDATE', 'Kitchen Update Status', 'kitchen');
