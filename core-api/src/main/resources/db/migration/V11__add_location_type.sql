-- Add location_type column to locations table
-- This allows different location types within the same zone

ALTER TABLE locations 
ADD COLUMN location_type VARCHAR(32) DEFAULT 'STORAGE';

-- Update existing locations based on their zone type
UPDATE locations loc
SET location_type = zones.zone_type
FROM zones
WHERE loc.zone_id = zones.id
  AND zones.zone_type IN ('RECEIVING', 'STORAGE', 'SHIPPING', 'QUARANTINE');

-- Set default to STORAGE for any locations without a matching zone type
UPDATE locations
SET location_type = 'STORAGE'
WHERE location_type IS NULL;

COMMENT ON COLUMN locations.location_type IS 'Type of location: RECEIVING, STORAGE, SHIPPING, QUARANTINE';
