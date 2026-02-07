-- Add outbound reference for cross-dock receipts and optimize shipping workflow queries.

ALTER TABLE receipts
ADD COLUMN IF NOT EXISTS outbound_ref VARCHAR(128);

COMMENT ON COLUMN receipts.outbound_ref IS 'Optional outbound document reference for cross-dock flow';

CREATE INDEX IF NOT EXISTS idx_receipts_status_crossdock_updated_at
    ON receipts(status, cross_dock, updated_at);

CREATE INDEX IF NOT EXISTS idx_tasks_receipt_tasktype_status
    ON tasks(receipt_id, task_type, status);

CREATE INDEX IF NOT EXISTS idx_pallets_receipt_status_location
    ON pallets(receipt_id, status, location_id);
