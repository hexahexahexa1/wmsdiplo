-- Table for storing import service configuration
CREATE TABLE import_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on config_key for faster lookups
CREATE INDEX idx_import_config_key ON import_config(config_key);

-- Insert default import folder configuration
INSERT INTO import_config (config_key, config_value, updated_at) 
VALUES ('import_folder', 'input', CURRENT_TIMESTAMP);
