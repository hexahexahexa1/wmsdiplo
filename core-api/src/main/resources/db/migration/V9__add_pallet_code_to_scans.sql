-- Add pallet_code column to scans table for terminal receiving workflow
ALTER TABLE scans ADD COLUMN pallet_code VARCHAR(64);

-- Add index for fast lookup of scans by pallet
CREATE INDEX idx_scans_pallet_code ON scans(pallet_code);

-- Add comments for documentation
COMMENT ON COLUMN scans.pallet_code IS 'Pallet code scanned during receiving. Links scan to specific pallet.';
