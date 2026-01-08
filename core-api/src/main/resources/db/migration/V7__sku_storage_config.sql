-- SKU storage configuration
CREATE TABLE sku_storage_config (
    id              BIGSERIAL PRIMARY KEY,
    sku_id          BIGINT NOT NULL UNIQUE,
    velocity_class  VARCHAR(1) DEFAULT 'C',
    preferred_zone_id BIGINT REFERENCES zones(id),
    hazmat_class    VARCHAR(16),
    temp_range      VARCHAR(32),
    min_stock       NUMERIC(18,3) DEFAULT 0,
    max_stock       NUMERIC(18,3),
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_sku_storage_config_velocity ON sku_storage_config(velocity_class);
