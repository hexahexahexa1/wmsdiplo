-- Add deterministic scan -> effect links for safe undo.

ALTER TABLE discrepancies
ADD COLUMN IF NOT EXISTS scan_id BIGINT;

ALTER TABLE pallet_movements
ADD COLUMN IF NOT EXISTS scan_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_discrepancies_scan_id'
    ) THEN
        ALTER TABLE discrepancies
            ADD CONSTRAINT fk_discrepancies_scan_id
            FOREIGN KEY (scan_id) REFERENCES scans(id) ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_pallet_movements_scan_id'
    ) THEN
        ALTER TABLE pallet_movements
            ADD CONSTRAINT fk_pallet_movements_scan_id
            FOREIGN KEY (scan_id) REFERENCES scans(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_discrepancies_scan_id ON discrepancies(scan_id);
CREATE INDEX IF NOT EXISTS idx_pallet_movements_scan_id ON pallet_movements(scan_id);
