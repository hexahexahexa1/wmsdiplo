-- Users and roles
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(256) NOT NULL,
    full_name       VARCHAR(255),
    email           VARCHAR(255),
    role            VARCHAR(32) NOT NULL DEFAULT 'OPERATOR',
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- Default admin user (password: admin, stored with noop for dev)
INSERT INTO users (username, password_hash, full_name, role, is_active)
SELECT 'admin', '{noop}admin', 'Administrator', 'ADMIN', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
