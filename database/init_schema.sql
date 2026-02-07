--
-- WMSDIPL Database Schema
-- Complete schema dump without migrations
-- Generated: 2026-01-12
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';
SET default_table_access_method = heap;

-- Drop all tables if they exist (for clean restore)
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS discrepancies CASCADE;
DROP TABLE IF EXISTS import_config CASCADE;
DROP TABLE IF EXISTS import_log CASCADE;
DROP TABLE IF EXISTS locations CASCADE;
DROP TABLE IF EXISTS packagings CASCADE;
DROP TABLE IF EXISTS pallet_code_pool CASCADE;
DROP TABLE IF EXISTS pallet_movements CASCADE;
DROP TABLE IF EXISTS pallets CASCADE;
DROP TABLE IF EXISTS putaway_rules CASCADE;
DROP TABLE IF EXISTS receipt_lines CASCADE;
DROP TABLE IF EXISTS receipts CASCADE;
DROP TABLE IF EXISTS scans CASCADE;
DROP TABLE IF EXISTS schema_version CASCADE;
DROP TABLE IF EXISTS sku_storage_config CASCADE;
DROP TABLE IF EXISTS sku_unit_configs CASCADE;
DROP TABLE IF EXISTS skus CASCADE;
DROP TABLE IF EXISTS status_history CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS zones CASCADE;

-- Drop types if they exist
DROP TYPE IF EXISTS location_status CASCADE;
DROP TYPE IF EXISTS location_type CASCADE;
DROP TYPE IF EXISTS pallet_status CASCADE;
DROP TYPE IF EXISTS receipt_status CASCADE;
DROP TYPE IF EXISTS task_status CASCADE;
DROP TYPE IF EXISTS task_type CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS movement_type CASCADE;

-- Create ENUM types
CREATE TYPE location_status AS ENUM ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'BLOCKED', 'MAINTENANCE');
CREATE TYPE location_type AS ENUM ('RECEIVING', 'STORAGE', 'PICKING', 'SHIPPING', 'CROSS_DOCK', 'DAMAGED', 'QUARANTINE');
CREATE TYPE pallet_status AS ENUM ('EMPTY', 'RECEIVING', 'RECEIVED', 'STORED', 'IN_TRANSIT', 'PLACED', 'PICKING', 'SHIPPED', 'DAMAGED', 'QUARANTINE');
CREATE TYPE receipt_status AS ENUM (
    'DRAFT',
    'CONFIRMED',
    'IN_PROGRESS',
    'ACCEPTED',
    'READY_FOR_PLACEMENT',
    'READY_FOR_SHIPMENT',
    'SHIPPING_IN_PROGRESS',
    'SHIPPED',
    'PLACING',
    'STOCKED',
    'CANCELLED'
);
CREATE TYPE task_status AS ENUM ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
CREATE TYPE task_type AS ENUM ('RECEIVING', 'PLACEMENT', 'SHIPPING', 'PUTAWAY', 'PICKING', 'REPLENISHMENT', 'CYCLE_COUNT');
CREATE TYPE user_role AS ENUM ('OPERATOR', 'SUPERVISOR', 'ADMIN');
CREATE TYPE movement_type AS ENUM ('RECEIVE', 'PUTAWAY', 'PICK', 'TRANSFER', 'ADJUSTMENT');

--
-- Table: zones
--
CREATE TABLE zones (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority_rank INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: locations
--
CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    zone_id BIGINT NOT NULL REFERENCES zones(id),
    location_type location_type NOT NULL,
    status location_status NOT NULL DEFAULT 'AVAILABLE',
    aisle VARCHAR(10),
    bay VARCHAR(10),
    level VARCHAR(10),
    x_coord INTEGER,
    y_coord INTEGER,
    z_coord INTEGER,
    max_pallets INTEGER DEFAULT 1,
    max_weight_kg NUMERIC(10,2),
    max_width_cm NUMERIC(8,2),
    max_height_cm NUMERIC(8,2),
    max_depth_cm NUMERIC(8,2),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: skus
--
CREATE TABLE skus (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    uom VARCHAR(10) NOT NULL,
    pallet_capacity NUMERIC(10,2)
);

--
-- Table: sku_unit_configs
--
CREATE TABLE sku_unit_configs (
    id BIGSERIAL PRIMARY KEY,
    sku_id BIGINT NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    unit_code VARCHAR(32) NOT NULL,
    factor_to_base NUMERIC(18,6) NOT NULL CHECK (factor_to_base > 0),
    units_per_pallet NUMERIC(18,3) NOT NULL CHECK (units_per_pallet > 0),
    is_base BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sku_id, unit_code)
);

--
-- Table: packagings
--
CREATE TABLE packagings (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50)
);

--
-- Table: receipts
--
CREATE TABLE receipts (
    id BIGSERIAL PRIMARY KEY,
    doc_no VARCHAR(50) NOT NULL UNIQUE,
    doc_date DATE NOT NULL,
    status receipt_status NOT NULL DEFAULT 'DRAFT',
    supplier VARCHAR(255),
    message_id VARCHAR(100),
    external_key VARCHAR(100),
    cross_dock BOOLEAN DEFAULT false,
    outbound_ref VARCHAR(128),
    entity_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: receipt_lines
--
CREATE TABLE receipt_lines (
    id BIGSERIAL PRIMARY KEY,
    receipt_id BIGINT NOT NULL REFERENCES receipts(id) ON DELETE CASCADE,
    line_no INTEGER NOT NULL,
    sku_id BIGINT NOT NULL REFERENCES skus(id),
    packaging_id BIGINT REFERENCES packagings(id),
    uom VARCHAR(10) NOT NULL,
    qty_expected NUMERIC(10,2) NOT NULL,
    qty_expected_base NUMERIC(18,3),
    unit_factor_to_base NUMERIC(18,6),
    units_per_pallet_snapshot NUMERIC(18,3),
    sscc_expected VARCHAR(50),
    lot_number_expected VARCHAR(100),
    expiry_date_expected DATE,
    UNIQUE(receipt_id, line_no)
);

--
-- Table: pallets
--
CREATE TABLE pallets (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    code_type VARCHAR(20) DEFAULT 'INTERNAL',
    sku_id BIGINT REFERENCES skus(id),
    receipt_id BIGINT REFERENCES receipts(id),
    receipt_line_id BIGINT REFERENCES receipt_lines(id),
    location_id BIGINT REFERENCES locations(id),
    status pallet_status NOT NULL DEFAULT 'EMPTY',
    quantity NUMERIC(10,2),
    uom VARCHAR(10),
    weight_kg NUMERIC(10,2),
    height_cm NUMERIC(8,2),
    lot_number VARCHAR(100),
    expiry_date DATE,
    entity_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: tasks
--
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    receipt_id BIGINT REFERENCES receipts(id),
    line_id BIGINT REFERENCES receipt_lines(id),
    task_type task_type NOT NULL,
    status task_status NOT NULL DEFAULT 'NEW',
    priority INTEGER DEFAULT 0,
    qty_assigned NUMERIC(10,2),
    qty_done NUMERIC(10,2) DEFAULT 0,
    assignee VARCHAR(100),
    assigned_by VARCHAR(100),
    pallet_id BIGINT REFERENCES pallets(id),
    source_location_id BIGINT REFERENCES locations(id),
    target_location_id BIGINT REFERENCES locations(id),
    entity_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    closed_at TIMESTAMP
);

--
-- Table: scans
--
CREATE TABLE scans (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id),
    request_id VARCHAR(128),
    pallet_code VARCHAR(50) NOT NULL,
    sscc VARCHAR(50),
    barcode VARCHAR(50),
    qty NUMERIC(10,2) NOT NULL,
    device_id VARCHAR(50),
    discrepancy BOOLEAN DEFAULT false,
    damage_flag BOOLEAN DEFAULT false,
    damage_type VARCHAR(50),
    damage_description TEXT,
    lot_number VARCHAR(100),
    expiry_date DATE,
    scanned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: discrepancies
--
CREATE TABLE discrepancies (
    id BIGSERIAL PRIMARY KEY,
    receipt_id BIGINT NOT NULL REFERENCES receipts(id),
    line_id BIGINT REFERENCES receipt_lines(id),
    task_id BIGINT REFERENCES tasks(id),
    pallet_id BIGINT REFERENCES pallets(id),
    type VARCHAR(50) NOT NULL,
    description TEXT,
    qty_expected NUMERIC(10,2),
    qty_actual NUMERIC(10,2),
    resolved BOOLEAN DEFAULT false,
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: users
--
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email VARCHAR(255),
    role user_role NOT NULL DEFAULT 'OPERATOR',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: putaway_rules
--
CREATE TABLE putaway_rules (
    id BIGSERIAL PRIMARY KEY,
    sku_id BIGINT REFERENCES skus(id),
    zone_id BIGINT REFERENCES zones(id),
    strategy VARCHAR(50) NOT NULL,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: sku_storage_config
--
CREATE TABLE sku_storage_config (
    id BIGSERIAL PRIMARY KEY,
    sku_id BIGINT NOT NULL REFERENCES skus(id),
    min_stock_level NUMERIC(10,2),
    max_stock_level NUMERIC(10,2),
    reorder_point NUMERIC(10,2),
    preferred_zone_id BIGINT REFERENCES zones(id),
    allow_mixing BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: pallet_code_pool
--
CREATE TABLE pallet_code_pool (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    is_used BOOLEAN DEFAULT false,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: audit_logs
--
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    changed_by VARCHAR(64),
    field_name VARCHAR(64),
    old_value VARCHAR(512),
    new_value VARCHAR(512),
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(256)
);

--
-- Table: status_history
--
CREATE TABLE status_history (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(100),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: pallet_movements
--
CREATE TABLE pallet_movements (
    id BIGSERIAL PRIMARY KEY,
    pallet_id BIGINT NOT NULL REFERENCES pallets(id),
    movement_type movement_type NOT NULL,
    from_location_id BIGINT REFERENCES locations(id),
    to_location_id BIGINT REFERENCES locations(id),
    quantity NUMERIC(10,2),
    task_id BIGINT,
    moved_by VARCHAR(128),
    moved_at TIMESTAMP
);

--
-- Table: import_config
--
CREATE TABLE import_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: import_log
--
CREATE TABLE import_log (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    message_id VARCHAR(100),
    receipt_id BIGINT REFERENCES receipts(id),
    error_message TEXT,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table: schema_version (Flyway compatibility - not used)
--
CREATE TABLE schema_version (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL
);

-- Indexes
CREATE INDEX idx_locations_zone ON locations(zone_id);
CREATE INDEX idx_locations_type ON locations(location_type);
CREATE INDEX idx_locations_status ON locations(status);
CREATE UNIQUE INDEX uq_sku_unit_configs_single_base ON sku_unit_configs(sku_id) WHERE is_base = true;
CREATE INDEX idx_sku_unit_configs_sku ON sku_unit_configs(sku_id);
CREATE INDEX idx_receipt_lines_receipt ON receipt_lines(receipt_id);
CREATE INDEX idx_receipt_lines_sku ON receipt_lines(sku_id);
CREATE INDEX idx_pallets_sku ON pallets(sku_id);
CREATE INDEX idx_pallets_location ON pallets(location_id);
CREATE INDEX idx_pallets_receipt ON pallets(receipt_id);
CREATE INDEX idx_pallets_status ON pallets(status);
CREATE INDEX idx_tasks_receipt ON tasks(receipt_id);
CREATE INDEX idx_tasks_assignee ON tasks(assignee);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_type ON tasks(task_type);
CREATE INDEX idx_scans_task ON scans(task_id);
CREATE INDEX idx_scans_pallet ON scans(pallet_code);
CREATE UNIQUE INDEX uq_scans_task_request_id ON scans(task_id, request_id) WHERE request_id IS NOT NULL;
CREATE INDEX idx_discrepancies_receipt ON discrepancies(receipt_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_status_history_entity ON status_history(entity_type, entity_id);
CREATE INDEX idx_movements_pallet ON pallet_movements(pallet_id);
CREATE INDEX idx_movements_task ON pallet_movements(task_id);
CREATE INDEX idx_import_config_key ON import_config(config_key);
CREATE INDEX idx_receipts_status ON receipts(status);
CREATE INDEX idx_receipts_doc_date ON receipts(doc_date);
CREATE INDEX idx_tasks_created ON tasks(created_at);
CREATE INDEX idx_receipts_status_crossdock_updated_at ON receipts(status, cross_dock, updated_at);
CREATE INDEX idx_tasks_receipt_tasktype_status ON tasks(receipt_id, task_type, status);
CREATE INDEX idx_pallets_receipt_status_location ON pallets(receipt_id, status, location_id);

-- Insert default import configuration
INSERT INTO import_config (config_key, config_value, updated_at) 
VALUES ('import_folder', 'E:\WMSDIPL\import-data\incoming', CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO import_config (config_key, config_value, updated_at) 
VALUES ('api_url', 'http://localhost:8080', CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;

-- Insert a dummy record into schema_version to prevent Flyway from running
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (1, '999', 'Manual schema - migrations disabled', 'SQL', 'manual_schema.sql', 0, 'manual', CURRENT_TIMESTAMP, 0, true);
