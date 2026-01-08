-- Rollback script for receipt RCP-NEW-SKU-001 (id=28)
-- This will reset the receipt to DRAFT status and remove all tasks, scans, and discrepancies

BEGIN;

-- 1. Delete all scans related to tasks for this receipt
DELETE FROM scans 
WHERE task_id IN (SELECT id FROM tasks WHERE receipt_id = 28);

-- 2. Delete all discrepancies for this receipt
DELETE FROM discrepancies 
WHERE receipt_id = 28;

-- 3. Delete all tasks for this receipt
DELETE FROM tasks 
WHERE receipt_id = 28;

-- 4. Delete all pallets created for this receipt (if any)
DELETE FROM pallets 
WHERE receipt_id = 28;

-- 5. Reset receipt status to DRAFT
UPDATE receipts 
SET status = 'DRAFT'
WHERE id = 28;

-- Verify the rollback
SELECT 'Receipt status after rollback:' as info;
SELECT id, doc_no, status FROM receipts WHERE id = 28;

SELECT 'Remaining tasks:' as info;
SELECT COUNT(*) as count FROM tasks WHERE receipt_id = 28;

SELECT 'Remaining scans:' as info;
SELECT COUNT(*) as count FROM scans WHERE task_id IN (SELECT id FROM tasks WHERE receipt_id = 28);

SELECT 'Remaining discrepancies:' as info;
SELECT COUNT(*) as count FROM discrepancies WHERE receipt_id = 28;

COMMIT;
