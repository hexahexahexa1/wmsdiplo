-- ========================================
-- WMS Terminal Testing - Test Data Script (FINAL)
-- ========================================

-- Clean up existing test data
DELETE FROM scans WHERE pallet_code LIKE 'PLT-TEST-%';
DELETE FROM discrepancies WHERE pallet_code LIKE 'PLT-TEST-%';
DELETE FROM tasks WHERE receipt_id IN (SELECT id FROM receipts WHERE doc_no LIKE 'ACT-TEST-%');
DELETE FROM receipt_lines WHERE receipt_id IN (SELECT id FROM receipts WHERE doc_no LIKE 'ACT-TEST-%');
DELETE FROM receipts WHERE doc_no LIKE 'ACT-TEST-%';
DELETE FROM pallets WHERE code LIKE 'PLT-TEST-%';
DELETE FROM skus WHERE code LIKE 'SKU-TEST-%';
DELETE FROM locations WHERE code LIKE 'TEST-%';
DELETE FROM zones WHERE code LIKE 'TEST-%';
DELETE FROM users WHERE username = 'testuser';

-- ========================================
-- 0. Create Test User
-- ========================================
INSERT INTO users (username, password_hash, full_name, role, is_active)
VALUES ('testuser', '{noop}password', 'Test User', 'OPERATOR', true);

-- ========================================
-- 1. Create Test Zone (RECEIVING)
-- ========================================
INSERT INTO zones (code, name, zone_type, is_active) 
VALUES ('TEST-RECEIVING-01', 'Test Receiving Zone', 'RECEIVING', true);

-- ========================================
-- 2. Create Test Locations in RECEIVING Zone
-- ========================================
INSERT INTO locations (code, zone_id, status, max_pallets, is_active)
SELECT 
    'TEST-RCV-001', 
    id, 
    'AVAILABLE', 
    10, 
    true
FROM zones WHERE code = 'TEST-RECEIVING-01';

INSERT INTO locations (code, zone_id, status, max_pallets, is_active)
SELECT 
    'TEST-RCV-002', 
    id, 
    'AVAILABLE', 
    10, 
    true
FROM zones WHERE code = 'TEST-RECEIVING-01';

-- ========================================
-- 3. Create Test SKUs
-- ========================================
INSERT INTO skus (code, name, uom) 
VALUES 
    ('SKU-TEST-001', 'Тестовый товар 1', 'PCS'),
    ('SKU-TEST-002', 'Тестовый товар 2', 'PCS'),
    ('SKU-TEST-003', 'Тестовый товар 3', 'PCS');

-- ========================================
-- 4. Create Test Pallets (empty)
-- ========================================
INSERT INTO pallets (code, status)
VALUES 
    ('PLT-TEST-001', 'EMPTY'),
    ('PLT-TEST-002', 'EMPTY'),
    ('PLT-TEST-003', 'EMPTY'),
    ('PLT-TEST-004', 'EMPTY'),
    ('PLT-TEST-005', 'EMPTY');

-- ========================================
-- 5. Create Test Receipts
-- ========================================
INSERT INTO receipts (doc_no, doc_date, supplier, status, message_id)
VALUES 
    ('ACT-TEST-2026-001', '2026-01-08', 'Тестовый поставщик А', 'IN_PROGRESS', 'MSG-TEST-001'),
    ('ACT-TEST-2026-002', '2026-01-08', 'Тестовый поставщик Б', 'IN_PROGRESS', 'MSG-TEST-002'),
    ('ACT-TEST-2026-003', '2026-01-08', 'Тестовый поставщик В', 'CONFIRMED', 'MSG-TEST-003');

-- ========================================
-- 6. Create Test Receipt Lines
-- ========================================

INSERT INTO receipt_lines (receipt_id, line_no, sku_id, uom, qty_expected)
SELECT 
    r.id, 
    1, 
    s.id, 
    'PCS', 
    100
FROM receipts r, skus s
WHERE r.doc_no = 'ACT-TEST-2026-001' AND s.code = 'SKU-TEST-001';

INSERT INTO receipt_lines (receipt_id, line_no, sku_id, uom, qty_expected)
SELECT 
    r.id, 
    2, 
    s.id, 
    'PCS', 
    50
FROM receipts r, skus s
WHERE r.doc_no = 'ACT-TEST-2026-001' AND s.code = 'SKU-TEST-002';

INSERT INTO receipt_lines (receipt_id, line_no, sku_id, uom, qty_expected)
SELECT 
    r.id, 
    1, 
    s.id, 
    'PCS', 
    200
FROM receipts r, skus s
WHERE r.doc_no = 'ACT-TEST-2026-002' AND s.code = 'SKU-TEST-002';

INSERT INTO receipt_lines (receipt_id, line_no, sku_id, uom, qty_expected)
SELECT 
    r.id, 
    1, 
    s.id, 
    'PCS', 
    75
FROM receipts r, skus s
WHERE r.doc_no = 'ACT-TEST-2026-003' AND s.code = 'SKU-TEST-003';

-- ========================================
-- 7. Create Test Tasks (RECEIVING type)
-- ========================================

-- Task 1: NEW status, not assigned
INSERT INTO tasks (task_type, status, receipt_id, line_id, qty_assigned, qty_done)
SELECT 
    'RECEIVING', 
    'NEW', 
    r.id, 
    rl.id, 
    100, 
    0
FROM receipts r
JOIN receipt_lines rl ON rl.receipt_id = r.id
WHERE r.doc_no = 'ACT-TEST-2026-001' AND rl.line_no = 1;

-- Task 2: IN_PROGRESS status, assigned to testuser
INSERT INTO tasks (task_type, status, receipt_id, line_id, assignee, assigned_by, qty_assigned, qty_done, started_at)
SELECT 
    'RECEIVING', 
    'IN_PROGRESS', 
    r.id, 
    rl.id, 
    'testuser',
    'admin',
    50, 
    0,
    NOW() - INTERVAL '30 minutes'
FROM receipts r
JOIN receipt_lines rl ON rl.receipt_id = r.id
WHERE r.doc_no = 'ACT-TEST-2026-001' AND rl.line_no = 2;

-- Task 3: IN_PROGRESS status, assigned to testuser, partially completed
INSERT INTO tasks (task_type, status, receipt_id, line_id, assignee, assigned_by, qty_assigned, qty_done, started_at)
SELECT 
    'RECEIVING', 
    'IN_PROGRESS', 
    r.id, 
    rl.id, 
    'testuser',
    'admin',
    200, 
    75,
    NOW() - INTERVAL '1 hour 30 minutes'
FROM receipts r
JOIN receipt_lines rl ON rl.receipt_id = r.id
WHERE r.doc_no = 'ACT-TEST-2026-002' AND rl.line_no = 1;

-- Task 4: IN_PROGRESS status, assigned to other user
INSERT INTO tasks (task_type, status, receipt_id, line_id, assignee, assigned_by, qty_assigned, qty_done, started_at)
SELECT 
    'RECEIVING', 
    'IN_PROGRESS', 
    r.id, 
    rl.id, 
    'otheruser',
    'admin',
    75, 
    0,
    NOW() - INTERVAL '2 hours'
FROM receipts r
JOIN receipt_lines rl ON rl.receipt_id = r.id
WHERE r.doc_no = 'ACT-TEST-2026-003' AND rl.line_no = 1;

-- ========================================
-- 8. Create historical scans for Task 3
-- ========================================

DO $$
DECLARE
    v_task_id BIGINT;
    v_pallet_id BIGINT;
    v_location_id BIGINT;
    v_sku_id BIGINT;
BEGIN
    -- Get IDs
    SELECT t.id INTO v_task_id
    FROM tasks t
    JOIN receipts r ON r.id = t.receipt_id
    WHERE r.doc_no = 'ACT-TEST-2026-002' AND t.assignee = 'testuser';
    
    SELECT id INTO v_pallet_id FROM pallets WHERE code = 'PLT-TEST-003';
    SELECT id INTO v_location_id FROM locations WHERE code = 'TEST-RCV-001';
    SELECT id INTO v_sku_id FROM skus WHERE code = 'SKU-TEST-002';
    
    -- Initialize pallet
    UPDATE pallets 
    SET 
        sku_id = v_sku_id,
        status = 'RECEIVING',
        location_id = v_location_id,
        quantity = 75
    WHERE id = v_pallet_id;
    
    -- Create scans
    INSERT INTO scans (task_id, pallet_code, barcode, qty, device_id, scanned_at)
    VALUES 
        (v_task_id, 'PLT-TEST-003', 'SKU-TEST-002', 25, 'TERMINAL-001', NOW() - INTERVAL '1 hour 15 minutes'),
        (v_task_id, 'PLT-TEST-003', 'SKU-TEST-002', 30, 'TERMINAL-001', NOW() - INTERVAL '45 minutes'),
        (v_task_id, 'PLT-TEST-003', 'SKU-TEST-002', 20, 'TERMINAL-001', NOW() - INTERVAL '20 minutes');
END $$;

-- ========================================
-- 9. Summary Report
-- ========================================

\echo '========================================'
\echo 'TEST DATA CREATED SUCCESSFULLY'
\echo '========================================'

SELECT 'ZONES' as object_type, COUNT(*) as count FROM zones WHERE code LIKE 'TEST-%'
UNION ALL
SELECT 'LOCATIONS', COUNT(*) FROM locations WHERE code LIKE 'TEST-%'
UNION ALL
SELECT 'SKUS', COUNT(*) FROM skus WHERE code LIKE 'SKU-TEST-%'
UNION ALL
SELECT 'PALLETS', COUNT(*) FROM pallets WHERE code LIKE 'PLT-TEST-%'
UNION ALL
SELECT 'RECEIPTS', COUNT(*) FROM receipts WHERE doc_no LIKE 'ACT-TEST-%'
UNION ALL
SELECT 'RECEIPT_LINES', COUNT(*) FROM receipt_lines WHERE receipt_id IN (SELECT id FROM receipts WHERE doc_no LIKE 'ACT-TEST-%')
UNION ALL
SELECT 'TASKS', COUNT(*) FROM tasks WHERE receipt_id IN (SELECT id FROM receipts WHERE doc_no LIKE 'ACT-TEST-%')
UNION ALL
SELECT 'SCANS', COUNT(*) FROM scans WHERE task_id IN (SELECT id FROM tasks WHERE receipt_id IN (SELECT id FROM receipts WHERE doc_no LIKE 'ACT-TEST-%'));

\echo '========================================'
\echo 'TASKS FOR USER: testuser'
\echo '========================================'

SELECT 
    t.id,
    t.task_type,
    t.status,
    r.doc_no as receipt,
    t.qty_assigned,
    t.qty_done,
    CONCAT(t.qty_done, ' / ', t.qty_assigned) as progress
FROM tasks t
JOIN receipts r ON r.id = t.receipt_id
WHERE t.assignee = 'testuser'
ORDER BY t.id;

\echo '========================================'
\echo 'TEST CREDENTIALS'
\echo '========================================'
\echo 'Username: testuser'
\echo 'Password: password'
\echo ''
\echo 'Available pallets: PLT-TEST-001, PLT-TEST-002, PLT-TEST-004, PLT-TEST-005'
\echo 'Used pallet: PLT-TEST-003 (SKU-TEST-002, qty=75)'
\echo ''
\echo 'Available SKU barcodes: SKU-TEST-001, SKU-TEST-002, SKU-TEST-003'
