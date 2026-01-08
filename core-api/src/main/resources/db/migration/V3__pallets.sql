-- Pallets and related structures
CREATE TABLE pallets (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(64) NOT NULL UNIQUE,
    code_type        VARCHAR(16) DEFAULT 'INTERNAL',
    status           VARCHAR(32) NOT NULL DEFAULT 'EMPTY',
    location_id      BIGINT REFERENCES locations(id),
    sku_id           BIGINT REFERENCES skus(id),
    lot_number       VARCHAR(64),
    expiry_date      DATE,
    quantity         NUMERIC(18,3),
    uom              VARCHAR(32),
    receipt_id       BIGINT REFERENCES receipts(id),
    receipt_line_id  BIGINT REFERENCES receipt_lines(id),
    weight_kg        NUMERIC(10,2),
    height_cm        NUMERIC(10,2),
    created_at       TIMESTAMP DEFAULT now(),
    updated_at       TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_pallets_status ON pallets(status);
CREATE INDEX idx_pallets_location ON pallets(location_id);
CREATE INDEX idx_pallets_sku ON pallets(sku_id);
CREATE INDEX idx_pallets_receipt ON pallets(receipt_id);

CREATE TABLE pallet_code_pool (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(64) NOT NULL UNIQUE,
    code_type     VARCHAR(16) DEFAULT 'INTERNAL',
    is_used       BOOLEAN DEFAULT FALSE,
    generated_at  TIMESTAMP DEFAULT now(),
    used_at       TIMESTAMP
);

CREATE TABLE pallet_movements (
    id                BIGSERIAL PRIMARY KEY,
    pallet_id         BIGINT NOT NULL REFERENCES pallets(id),
    from_location_id  BIGINT REFERENCES locations(id),
    to_location_id    BIGINT REFERENCES locations(id),
    movement_type     VARCHAR(32),
    task_id           BIGINT REFERENCES tasks(id),
    moved_by          VARCHAR(128),
    moved_at          TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_pallet_movements_pallet ON pallet_movements(pallet_id);
