CREATE TABLE IF NOT EXISTS skus (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    uom             VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS packagings (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    capacity        NUMERIC(18,2),
    uom             VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS receipts (
    id              BIGSERIAL PRIMARY KEY,
    external_key    VARCHAR(128),
    doc_no          VARCHAR(64) NOT NULL,
    doc_date        DATE,
    supplier        VARCHAR(255),
    status          VARCHAR(32) NOT NULL,
    message_id      VARCHAR(128),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT receipts_message_id_unique UNIQUE (message_id),
    CONSTRAINT receipts_doc_supplier UNIQUE (doc_no, supplier)
);

CREATE TABLE IF NOT EXISTS receipt_lines (
    id              BIGSERIAL PRIMARY KEY,
    receipt_id      BIGINT NOT NULL REFERENCES receipts(id) ON DELETE CASCADE,
    sku_id          BIGINT REFERENCES skus(id),
    packaging_id    BIGINT REFERENCES packagings(id),
    uom             VARCHAR(32),
    qty_expected    NUMERIC(18,3) NOT NULL,
    sscc_expected   VARCHAR(64),
    line_no         INTEGER
);

CREATE TABLE IF NOT EXISTS tasks (
    id              BIGSERIAL PRIMARY KEY,
    receipt_id      BIGINT NOT NULL REFERENCES receipts(id) ON DELETE CASCADE,
    line_id         BIGINT REFERENCES receipt_lines(id) ON DELETE SET NULL,
    assignee        VARCHAR(128),
    status          VARCHAR(32) NOT NULL,
    qty_assigned    NUMERIC(18,3),
    qty_done        NUMERIC(18,3),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    closed_at       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scans (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT REFERENCES tasks(id) ON DELETE CASCADE,
    sscc            VARCHAR(64),
    barcode         VARCHAR(128),
    qty             NUMERIC(18,3),
    device_id       VARCHAR(128),
    discrepancy     BOOLEAN DEFAULT FALSE,
    scanned_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS discrepancies (
    id              BIGSERIAL PRIMARY KEY,
    receipt_id      BIGINT NOT NULL REFERENCES receipts(id) ON DELETE CASCADE,
    line_id         BIGINT REFERENCES receipt_lines(id) ON DELETE SET NULL,
    type            VARCHAR(64),
    qty_expected    NUMERIC(18,3),
    qty_actual      NUMERIC(18,3),
    comment         VARCHAR(512),
    resolved        BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS import_log (
    id              BIGSERIAL PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    checksum        VARCHAR(128),
    message_id      VARCHAR(128),
    status          VARCHAR(32) NOT NULL,
    error_message   TEXT,
    processed_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT import_log_message_id_unique UNIQUE (message_id)
);

CREATE TABLE IF NOT EXISTS status_history (
    id              BIGSERIAL PRIMARY KEY,
    entity_id       BIGINT NOT NULL,
    entity_type     VARCHAR(64) NOT NULL,
    status_from     VARCHAR(32),
    status_to       VARCHAR(32) NOT NULL,
    changed_by      VARCHAR(128),
    changed_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_receipts_status ON receipts(status);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
