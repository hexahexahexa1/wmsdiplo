-- Check and fix stock movement history
-- Run this script to diagnose and repair stock history issues

-- 1. Check if pallet_movements table exists
SELECT EXISTS (
    SELECT FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name = 'pallet_movements'
) AS table_exists;

-- 2. Count existing movement records
SELECT COUNT(*) AS total_movements FROM pallet_movements;

-- 3. Count pallets
SELECT COUNT(*) AS total_pallets FROM pallets;

-- 4. Check which pallets have movements
SELECT 
    p.id AS pallet_id,
    p.code AS pallet_code,
    p.status,
    COUNT(pm.id) AS movement_count
FROM pallets p
LEFT JOIN pallet_movements pm ON pm.pallet_id = p.id
GROUP BY p.id, p.code, p.status
ORDER BY p.id
LIMIT 10;

-- 5. If no movements exist, create sample history from tasks/scans
-- This is a one-time migration query to populate history

-- Insert RECEIVE movements from existing scans
INSERT INTO pallet_movements (pallet_id, from_location_id, to_location_id, movement_type, task_id, moved_by, moved_at)
SELECT DISTINCT ON (p.id)
    p.id AS pallet_id,
    NULL AS from_location_id,
    1 AS to_location_id, -- Assuming location ID 1 is receiving area (adjust if needed)
    'RECEIVE' AS movement_type,
    t.id AS task_id,
    COALESCE(t.assignee, 'system') AS moved_by,
    COALESCE(s.created_at, p.created_at) AS moved_at
FROM pallets p
INNER JOIN scans s ON s.pallet_code = p.code
INNER JOIN tasks t ON t.id = s.task_id
WHERE t.task_type = 'RECEIVING'
  AND p.status IN ('RECEIVED', 'PLACED', 'PICKING')
  AND NOT EXISTS (
      SELECT 1 FROM pallet_movements pm 
      WHERE pm.pallet_id = p.id 
      AND pm.movement_type = 'RECEIVE'
  )
ORDER BY p.id, s.created_at ASC
ON CONFLICT DO NOTHING;

-- Insert PLACE movements from placement tasks
INSERT INTO pallet_movements (pallet_id, from_location_id, to_location_id, movement_type, task_id, moved_by, moved_at)
SELECT DISTINCT ON (p.id)
    p.id AS pallet_id,
    1 AS from_location_id, -- Receiving area
    p.location_id AS to_location_id,
    'PLACE' AS movement_type,
    t.id AS task_id,
    COALESCE(t.assignee, 'system') AS moved_by,
    COALESCE(t.completed_at, t.updated_at, p.updated_at) AS moved_at
FROM pallets p
INNER JOIN tasks t ON t.pallet_id = p.id
WHERE t.task_type = 'PLACEMENT'
  AND t.status = 'COMPLETED'
  AND p.location_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM pallet_movements pm 
      WHERE pm.pallet_id = p.id 
      AND pm.movement_type = 'PLACE'
  )
ORDER BY p.id, t.completed_at ASC
ON CONFLICT DO NOTHING;

-- 6. Verify movements were created
SELECT 
    pm.id,
    pm.movement_type,
    p.code AS pallet_code,
    l_from.code AS from_location,
    l_to.code AS to_location,
    pm.moved_by,
    pm.moved_at
FROM pallet_movements pm
INNER JOIN pallets p ON p.id = pm.pallet_id
LEFT JOIN locations l_from ON l_from.id = pm.from_location_id
LEFT JOIN locations l_to ON l_to.id = pm.to_location_id
ORDER BY pm.moved_at DESC
LIMIT 20;
