-- ========================================
-- WMS Terminal Testing - Test Data Script
-- ========================================

-- Clean up existing test data (in reverse order of dependencies)
DELETE FROM scans WHERE task_id IN (SELECT id FROM tasks WHERE receipt_id IN (SELECT id FROM receipts WHERE doc_no LIKE 'ACT-TEST-%'));
DELETE FROM discrepancies WHERE scan_id IN (SELECT id FROM scans WHERE task_id IN (SELECT id FROM tasks WHERE receipt_id IN (SELECT id FROM receipts WHERE doc_no LIKE 'ACT-TEST-%')));
DELETE FROM tasks WHERE receipt_id IN (SELECT id FROM receipts WHERE doc_no LIKE 'ACT-TEST-%');
DELETE FROM receipt_lines WHERE receipt_id IN (SELECT id FROM receipts WHERE doc_no LIKE 'ACT-TEST-%');
DELETE FROM receipts WHERE doc_no LIKE 'ACT-TEST-%';
DELETE FROM pallets WHERE code LIKE 'PLT-TEST-%';
DELETE FROM skus WHERE code LIKE 'SKU-TEST-%';
DELETE FROM locations WHERE code LIKE 'TEST-%';
DELETE FROM zones WHERE code LIKE 'TEST-%';

-- ========================================
-- 1. Create Test Zone (RECEIVING)
-- ========================================
INSERT INTO zones (code, zone_type, active, description) 
VALUES ('TEST-RECEIVING-01', 'RECEIVING', true, 'Test receiving zone for terminal');

-- ========================================
-- 2. Create Test Locations in RECEIVING Zone
-- ========================================
INSERT INTO locations (code, zone_id, status, max_pallets, active, description)
SELECT 
    'TEST-RCV-001', 
    id, 
    'AVAILABLE', 
    10, 
    true,
    'Test receiving location 1'
FROM zones WHERE code = 'TEST-RECEIVING-01';

INSERT INTO locations (code, zone_id, status, max_pallets, active, description)
SELECT 
    'TEST-RCV-002', 
    id, 
    'AVAILABLE', 
    10, 
    true,
    'Test receiving location 2'
FROM zones WHERE code = 'TEST-RECEIVING-01';

-- ========================================
-- 3. Create Test SKUs
-- ========================================
INSERT INTO skus (code, name, active, description) 
VALUES 
    ('SKU-TEST-001', 'Тестовый товар 1', true, 'Test product for terminal testing'),
    ('SKU-TEST-002', 'Тестовый товар 2', true, 'Second test product'),
    ('SKU-TEST-003', 'Тестовый товар 3', true, 'Third test product');

-- ========================================
-- 4. Create Test Pallets (empty, will be initialized on first scan)
-- ========================================
INSERT INTO pallets (code, status, active)
VALUES 
    ('PLT-TEST-001', 'AVAILABLE', true),
    ('PLT-TEST-002', 'AVAILABLE', true),
    ('PLT-TEST-003', 'AVAILABLE', true),
    ('PLT-TEST-004', 'AVAILABLE', true),
    ('PLT-TEST-005', 'AVAILABLE', true);

-- ========================================
-- 5. Create Test Receipts
-- ========================================
INSERT INTO receipts (doc_no, doc_date, supplier, status, message_id)
VALUES 
    ('ACT-TEST-2026-001', '2026-01-08', 'Тестовый поставщик А', 'RECEIVING', 'MSG-TEST-001'),
    ('ACT-TEST-2026-002', '2026-01-08', 'Тестовый поставщик Б', 'RECEIVING', 'MSG-TEST-002'),
    ('ACT-TEST-2026-003', '2026-01-08', 'Тестовый поставщик В', 'CONFIRMED', 'MSG-TEST-003');

-- ========================================
-- 6. Create Test Receipt Lines
-- ========================================

-- Receipt 1 - Lines for ACT-TEST-2026-001
INSERT INTO receipt_lines (receipt_id, line_no, sku_id, uom, qty_expected, qty_done)
SELECT 
    r.id, 
    1, 
    s.id, 
    'PCS', 
    100, 
    0
FROM receipts r, skus s
WHERE r.doc_no = 'ACT-TEST-2026-001' AND s.code = 'SKU-TEST-001';

INSERT INTO receipt_lines (receipt_id, line_no, sku_id, uom, qty_expected, qty_done)
SELECT 
    r.id, 
    2, 
    s.id, 
    'PCS', 
    50, 
    0
FROM receipts r, skus s
WHERE r.doc_no = 'ACT-TEST-2026-001' AND s.code = 'SKU-TEST-002';

-- Receipt 2 - Lines for ACT-TEST-2026-002
INSERT INTO receipt_lines (receipt_id, line_no, sku_id, uom, qty_expected, qty_done)
SELECT 
    r.id, 
    1, 
    s.id, 
    'PCS', 
    200, 
    0
FROM receipts r, skus s
WHERE r.doc_no = 'ACT-TEST-2026-002' AND s.code = 'SKU-TEST-002';

-- Receipt 3 - Lines for ACT-TEST-2026-003 (CONFIRMED status - no tasks yet)
INSERT INTO receipt_lines (receipt_id, line_no, sku_id, uom, qty_expected, qty_done)
SELECT 
    r.id, 
    1, 
    s.id, 
    'PCS', 
    75, 
    0
FROM receipts r, skus s
WHERE r.doc_no = 'ACT-TEST-2026-003' AND s.code = 'SKU-TEST-003';

-- ========================================
-- 7. Create Test Tasks (RECEIVING type)
-- ========================================

-- Task 1: NEW status, not assigned (will be assigned to testuser)
INSERT INTO tasks (task_type, status, receipt_id, line_id, assignee, assigned_by, qty_assigned, qty_done, created_at)
SELECT 
    'RECEIVING', 
    'NEW', 
    r.id, 
    rl.id, 
    NULL,
    NULL,
    100, 
    0, 
    NOW()
FROM receipts r
JOIN receipt_lines rl ON rl.receipt_id = r.id
WHERE r.doc_no = 'ACT-TEST-2026-001' AND rl.line_no = 1;

-- Task 2: IN_PROGRESS status, assigned to testuser (ready for scanning)
INSERT INTO tasks (task_type, status, receipt_id, line_id, assignee, assigned_by, qty_assigned, qty_done, created_at, started_at)
SELECT 
    'RECEIVING', 
    'IN_PROGRESS', 
    r.id, 
    rl.id, 
    'testuser',
    'admin',
    50, 
    0, 
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '30 minutes'
FROM receipts r
JOIN receipt_lines rl ON rl.receipt_id = r.id
WHERE r.doc_no = 'ACT-TEST-2026-001' AND rl.line_no = 2;

-- Task 3: IN_PROGRESS status, assigned to testuser, partially completed
INSERT INTO tasks (task_type, status, receipt_id, line_id, assignee, assigned_by, qty_assigned, qty_done, created_at, started_at)
SELECT 
    'RECEIVING', 
    'IN_PROGRESS', 
    r.id, 
    rl.id, 
    'testuser',
    'admin',
    200, 
    75, 
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour 30 minutes'
FROM receipts r
JOIN receipt_lines rl ON rl.receipt_id = r.id
WHERE r.doc_no = 'ACT-TEST-2026-002' AND rl.line_no = 1;

-- Task 4: NEW status, assigned to another user (should not appear in "My Tasks")
INSERT INTO tasks (task_type, status, receipt_id, line_id, assignee, assigned_by, qty_assigned, qty_done, created_at)
SELECT 
    'RECEIVING', 
    'IN_PROGRESS', 
    r.id, 
    rl.id, 
    'otheruser',
    'admin',
    75, 
    0, 
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '2 hours'
FROM receipts r
JOIN receipt_lines rl ON rl.receipt_id = r.id
WHERE r.doc_no = 'ACT-TEST-2026-003' AND rl.line_no = 1;

-- ========================================
-- 8. Create some historical scans for Task 3 (partially completed)
-- ========================================

-- Get task_id for the partially completed task
DO $$
DECLARE
    v_task_id BIGINT;
    v_pallet_id BIGINT;
    v_location_id BIGINT;
    v_sku_id BIGINT;
BEGIN
    -- Get task ID
    SELECT t.id INTO v_task_id
    FROM tasks t
    JOIN receipts r ON r.id = t.receipt_id
    WHERE r.doc_no = 'ACT-TEST-2026-002' AND t.assignee = 'testuser';
    
    -- Get pallet ID
    SELECT id INTO v_pallet_id FROM pallets WHERE code = 'PLT-TEST-003';
    
    -- Get location ID
    SELECT id INTO v_location_id FROM locations WHERE code = 'TEST-RCV-001';
    
    -- Get SKU ID
    SELECT id INTO v_sku_id FROM skus WHERE code = 'SKU-TEST-002';
    
    -- Initialize pallet PLT-TEST-003 (already used)
    UPDATE pallets 
    SET 
        sku_id = v_sku_id,
        status = 'RECEIVING',
        location_id = v_location_id,
        quantity = 75
    WHERE id = v_pallet_id;
    
    -- Create 3 historical scans
    INSERT INTO scans (task_id, pallet_code, sscc, barcode, qty, device_id, scanned_at)
    VALUES 
        (v_task_id, 'PLT-TEST-003', NULL, 'SKU-TEST-002', 25, 'TERMINAL-001', NOW() - INTERVAL '1 hour 15 minutes'),
        (v_task_id, 'PLT-TEST-003', NULL, 'SKU-TEST-002', 30, 'TERMINAL-001', NOW() - INTERVAL '45 minutes'),
        (v_task_id, 'PLT-TEST-003', NULL, 'SKU-TEST-002', 20, 'TERMINAL-001', NOW() - INTERVAL '20 minutes');
END $$;

-- ========================================
-- 9. Summary Report
-- ========================================

SELECT 
    '========================================' as divider,
    'TEST DATA CREATED SUCCESSFULLY' as status,
    '========================================' as divider2;

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

-- Show tasks for testuser
SELECT 
    '========================================' as divider,
    'TASKS FOR USER: testuser' as info,
    '========================================' as divider2;

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

-- Test credentials reminder
SELECT 
    '========================================' as divider,
    'TEST CREDENTIALS' as info,
    '========================================' as divider2;

SELECT 
    'Username: testuser' as credential
UNION ALL
SELECT 'Password: password'
UNION ALL
SELECT ''
UNION ALL
SELECT 'Available pallets: PLT-TEST-001, PLT-TEST-002, PLT-TEST-004, PLT-TEST-005'
UNION ALL
SELECT 'Used pallet: PLT-TEST-003 (SKU-TEST-002, qty=75)'
UNION ALL
SELECT ''
UNION ALL
SELECT 'Available SKU barcodes: SKU-TEST-001, SKU-TEST-002, SKU-TEST-003';
