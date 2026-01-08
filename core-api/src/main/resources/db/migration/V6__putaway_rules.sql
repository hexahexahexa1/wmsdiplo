-- Putaway rules for placement strategies
CREATE TABLE putaway_rules (
    id              BIGSERIAL PRIMARY KEY,
    priority        INTEGER NOT NULL DEFAULT 100,
    name            VARCHAR(128) NOT NULL,
    strategy_type   VARCHAR(32) NOT NULL,  -- CLOSEST, ABC, CONSOLIDATION, FIFO
    zone_id         BIGINT REFERENCES zones(id),
    sku_category    VARCHAR(64),
    velocity_class  VARCHAR(1),
    params          JSONB,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_putaway_rules_active ON putaway_rules(is_active);
CREATE INDEX idx_putaway_rules_priority ON putaway_rules(priority);

-- Insert default fallback rule
INSERT INTO putaway_rules (priority, name, strategy_type, is_active)
VALUES (100, 'Default fallback', 'CLOSEST', TRUE);
