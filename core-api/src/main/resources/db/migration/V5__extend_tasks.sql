-- Extend tasks table and add indices
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS task_type VARCHAR(32) DEFAULT 'RECEIVING';
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS pallet_id BIGINT REFERENCES pallets(id);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS source_location_id BIGINT REFERENCES locations(id);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS target_location_id BIGINT REFERENCES locations(id);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS assigned_by VARCHAR(128);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 100;

CREATE INDEX IF NOT EXISTS idx_tasks_type ON tasks(task_type);
CREATE INDEX IF NOT EXISTS idx_tasks_pallet ON tasks(pallet_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assignee ON tasks(assignee);
