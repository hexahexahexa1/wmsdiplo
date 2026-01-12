-- V022: Receiving Workflow Improvements
-- Date: 2026-01-12
-- Description: Add support for:
--   - Multi-pallet auto-split (palletCapacity in SKUs)
--   - Cross-docking (crossDock flag in Receipts)
--   - Lot tracking (lot numbers and expiry dates)
--   - Damaged goods tracking

-- 1. Add palletCapacity to SKUs for multi-pallet auto-split
ALTER TABLE skus 
ADD COLUMN pallet_capacity NUMERIC(10,2);

COMMENT ON COLUMN skus.pallet_capacity IS 'Standard pallet quantity for auto-splitting large receipt lines into multiple tasks';

-- 2. Add cross-dock flag to Receipts
ALTER TABLE receipts 
ADD COLUMN cross_dock BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN receipts.cross_dock IS 'True if receipt should bypass storage (cross-docking workflow)';

-- 3. Add lot tracking to ReceiptLines
ALTER TABLE receipt_lines 
ADD COLUMN lot_number_expected VARCHAR(128),
ADD COLUMN expiry_date_expected DATE;

COMMENT ON COLUMN receipt_lines.lot_number_expected IS 'Expected lot/batch number from supplier document';
COMMENT ON COLUMN receipt_lines.expiry_date_expected IS 'Expected expiry date from supplier document';

-- 4. Add damage tracking and lot tracking to Scans
ALTER TABLE scans 
ADD COLUMN damage_flag BOOLEAN DEFAULT FALSE,
ADD COLUMN damage_type VARCHAR(64),
ADD COLUMN damage_description VARCHAR(512),
ADD COLUMN lot_number VARCHAR(128),
ADD COLUMN expiry_date DATE;

COMMENT ON COLUMN scans.damage_flag IS 'True if goods are damaged during receiving';
COMMENT ON COLUMN scans.damage_type IS 'Damage type: PHYSICAL_DAMAGE, WATER_DAMAGE, EXPIRED, TEMPERATURE_ABUSE, CONTAMINATION, OTHER';
COMMENT ON COLUMN scans.damage_description IS 'Free text description of damage';
COMMENT ON COLUMN scans.lot_number IS 'Scanned lot/batch number';
COMMENT ON COLUMN scans.expiry_date IS 'Scanned expiry date';

-- 5. Add performance indexes
CREATE INDEX idx_scans_damage_flag ON scans(damage_flag) WHERE damage_flag = TRUE;
CREATE INDEX idx_scans_lot_number ON scans(lot_number);
CREATE INDEX idx_pallets_lot_number ON pallets(lot_number);
CREATE INDEX idx_tasks_priority ON tasks(priority DESC, created_at ASC);
CREATE INDEX idx_receipts_status_dates ON receipts(status, created_at);
CREATE INDEX idx_discrepancies_type ON discrepancies(type);
