ALTER TABLE skus
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_skus_status_code ON skus(status, code);

ALTER TABLE receipt_lines
    ADD COLUMN IF NOT EXISTS excluded_from_workflow BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE receipt_lines
    ADD COLUMN IF NOT EXISTS exclusion_reason VARCHAR(255);

ALTER TABLE discrepancies
    ADD COLUMN IF NOT EXISTS system_comment_key VARCHAR(128);

ALTER TABLE discrepancies
    ADD COLUMN IF NOT EXISTS system_comment_params TEXT;

ALTER TABLE discrepancies
    ADD COLUMN IF NOT EXISTS draft_sku_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_discrepancies_draft_sku'
    ) THEN
        ALTER TABLE discrepancies
            ADD CONSTRAINT fk_discrepancies_draft_sku
            FOREIGN KEY (draft_sku_id) REFERENCES skus(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_discrepancies_type_created_at ON discrepancies(type, created_at);
CREATE INDEX IF NOT EXISTS idx_discrepancies_draft_sku_id ON discrepancies(draft_sku_id);
