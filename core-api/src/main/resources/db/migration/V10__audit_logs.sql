-- Create audit_logs table for tracking entity changes
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    changed_by VARCHAR(64),
    field_name VARCHAR(64),
    old_value VARCHAR(512),
    new_value VARCHAR(512),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(256)
);

-- Create indexes for common queries
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_changed_by ON audit_logs(changed_by);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);

-- Add comments
COMMENT ON TABLE audit_logs IS 'Audit trail for all entity changes in the system';
COMMENT ON COLUMN audit_logs.entity_type IS 'Type of entity (e.g., Receipt, Pallet, Task)';
COMMENT ON COLUMN audit_logs.entity_id IS 'ID of the entity that was changed';
COMMENT ON COLUMN audit_logs.action IS 'Action performed (CREATE, UPDATE, DELETE, etc.)';
COMMENT ON COLUMN audit_logs.changed_by IS 'Username of the person who made the change';
COMMENT ON COLUMN audit_logs.field_name IS 'Name of the field that was changed (for UPDATE actions)';
COMMENT ON COLUMN audit_logs.old_value IS 'Previous value of the field';
COMMENT ON COLUMN audit_logs.new_value IS 'New value of the field';
