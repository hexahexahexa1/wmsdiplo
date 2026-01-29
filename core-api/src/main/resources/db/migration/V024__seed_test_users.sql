INSERT INTO users (username, password_hash, full_name, role, is_active)
SELECT 'supervisor', '{noop}supervisor', 'Shift Supervisor', 'SUPERVISOR', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'supervisor');

INSERT INTO users (username, password_hash, full_name, role, is_active)
SELECT 'operator', '{noop}operator', 'Warehouse Operator', 'OPERATOR', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'operator');

INSERT INTO users (username, password_hash, full_name, role, is_active)
SELECT 'pcoperator', '{noop}pcoperator', 'PC Operator', 'PC_OPERATOR', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'pcoperator');
