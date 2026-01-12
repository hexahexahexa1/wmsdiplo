-- V023: Add new location types for cross-docking and damaged goods
-- Date: 2026-01-12
-- Description: Support for CROSS_DOCK and DAMAGED location types

-- Note: LocationType is an enum in Java code, stored as VARCHAR in database
-- The enum values CROSS_DOCK and DAMAGED will be accepted automatically
-- This migration just adds clarity via comments

COMMENT ON COLUMN locations.location_type IS 'Location type: RECEIVING, STORAGE, SHIPPING, CROSS_DOCK, DAMAGED';

-- Optional: Insert sample locations for new types
-- Uncomment these if you want to pre-create locations during migration

-- INSERT INTO locations (code, name, location_type, status, active, zone_id) 
-- VALUES 
--   ('XDOCK-01', 'Cross-Dock Zone 1', 'CROSS_DOCK', 'AVAILABLE', true, (SELECT id FROM zones WHERE code = 'ZONE-A' LIMIT 1)),
--   ('DAMAGE-01', 'Damaged Goods Zone 1', 'DAMAGED', 'AVAILABLE', true, (SELECT id FROM zones WHERE code = 'ZONE-A' LIMIT 1));
