-- V12: Remove zone_type column from zones table
-- Location types are now the primary classification, zone types are no longer needed

-- Drop the zone_type column
ALTER TABLE zones DROP COLUMN IF EXISTS zone_type;
