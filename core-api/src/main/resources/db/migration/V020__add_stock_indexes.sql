-- Stock Inventory Feature: Performance indexes for stock queries
-- Migration: V020__add_stock_indexes.sql
-- Date: 2026-01-11

-- Indexes on pallets table for filtering and joins
CREATE INDEX IF NOT EXISTS idx_pallet_sku_id ON pallets(sku_id);
CREATE INDEX IF NOT EXISTS idx_pallet_location_id ON pallets(location_id);
CREATE INDEX IF NOT EXISTS idx_pallet_receipt_id ON pallets(receipt_id);
CREATE INDEX IF NOT EXISTS idx_pallet_status ON pallets(status);

-- Composite index for common query pattern (SKU + Location filtering on active pallets)
CREATE INDEX IF NOT EXISTS idx_pallet_sku_location ON pallets(sku_id, location_id) WHERE quantity > 0;

-- Indexes on pallet_movements table for history queries
CREATE INDEX IF NOT EXISTS idx_movement_pallet_id ON pallet_movements(pallet_id);
CREATE INDEX IF NOT EXISTS idx_movement_moved_at ON pallet_movements(moved_at);

-- Composite index for point-in-time queries (pallet + timestamp)
CREATE INDEX IF NOT EXISTS idx_movement_pallet_time ON pallet_movements(pallet_id, moved_at DESC);

-- Add quantity column to pallet_movements for future partial pick tracking
ALTER TABLE pallet_movements ADD COLUMN IF NOT EXISTS quantity NUMERIC(10,2);

COMMENT ON COLUMN pallet_movements.quantity IS 'Quantity involved in this movement (for partial picks/adjustments). NULL means full pallet moved.';
