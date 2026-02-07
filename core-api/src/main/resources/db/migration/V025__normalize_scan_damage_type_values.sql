-- Normalize legacy scan damage_type values to canonical enum-compatible values.
-- This keeps persisted data compatible with strict enum mapping used by the API.
UPDATE scans
SET damage_type = 'PHYSICAL_DAMAGE'
WHERE damage_type IS NOT NULL
  AND UPPER(TRIM(damage_type)) IN ('PHYSICAL', 'PHYSICAL_DAMAGE');

UPDATE scans
SET damage_type = 'WATER_DAMAGE'
WHERE damage_type IS NOT NULL
  AND UPPER(TRIM(damage_type)) IN ('WATER', 'WATER_DAMAGE');

UPDATE scans
SET damage_type = 'EXPIRED'
WHERE damage_type IS NOT NULL
  AND UPPER(TRIM(damage_type)) IN ('EXPIRED', 'EXPIRED_PRODUCT');

UPDATE scans
SET damage_type = 'TEMPERATURE_ABUSE'
WHERE damage_type IS NOT NULL
  AND UPPER(TRIM(damage_type)) IN ('TEMPERATURE', 'TEMP_ABUSE', 'TEMPERATURE_ABUSE');

UPDATE scans
SET damage_type = 'CONTAMINATION'
WHERE damage_type IS NOT NULL
  AND UPPER(TRIM(damage_type)) = 'CONTAMINATION';

UPDATE scans
SET damage_type = 'OTHER'
WHERE damage_type IS NOT NULL
  AND UPPER(TRIM(damage_type)) = 'OTHER';
