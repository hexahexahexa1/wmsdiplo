-- Warehouse topology: zones and locations
CREATE TABLE zones (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(32) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    zone_type       VARCHAR(32) NOT NULL DEFAULT 'STORAGE',
    priority_rank   INTEGER DEFAULT 100,
    description     VARCHAR(512),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE TABLE locations (
    id              BIGSERIAL PRIMARY KEY,
    zone_id         BIGINT NOT NULL REFERENCES zones(id),
    code            VARCHAR(32) NOT NULL UNIQUE,
    aisle           VARCHAR(8),
    bay             VARCHAR(8),
    level           VARCHAR(8),
    x_coord         NUMERIC(10,2),
    y_coord         NUMERIC(10,2),
    z_coord         NUMERIC(10,2),
    max_weight_kg   NUMERIC(10,2),
    max_height_cm   NUMERIC(10,2),
    max_width_cm    NUMERIC(10,2),
    max_depth_cm    NUMERIC(10,2),
    max_pallets     INTEGER DEFAULT 1,
    status          VARCHAR(32) DEFAULT 'AVAILABLE',
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_locations_zone ON locations(zone_id);
CREATE INDEX idx_locations_status ON locations(status);
CREATE INDEX idx_locations_code ON locations(code);
