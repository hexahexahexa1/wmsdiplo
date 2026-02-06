-- Reliability patch for existing databases (without full schema re-init)
-- Adds optimistic-lock columns and scan idempotency support.

ALTER TABLE receipts ADD COLUMN IF NOT EXISTS entity_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pallets ADD COLUMN IF NOT EXISTS entity_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS entity_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE scans ADD COLUMN IF NOT EXISTS request_id VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uq_scans_task_request_id
    ON scans(task_id, request_id)
    WHERE request_id IS NOT NULL;
