-- Fix legacy movement type values
-- This migration fixes movement_type values that don't match the MovementType enum

-- Update legacy PLACEMENT to PLACE
UPDATE pallet_movements 
SET movement_type = 'PLACE' 
WHERE movement_type = 'PLACEMENT';

-- Update legacy RECEIVING to RECEIVE (if any)
UPDATE pallet_movements 
SET movement_type = 'RECEIVE' 
WHERE movement_type = 'RECEIVING';

-- Update legacy MOVING to MOVE (if any)
UPDATE pallet_movements 
SET movement_type = 'MOVE' 
WHERE movement_type = 'MOVING';

-- Update legacy PICKING to PICK (if any)
UPDATE pallet_movements 
SET movement_type = 'PICK' 
WHERE movement_type = 'PICKING';

-- Verify results
SELECT movement_type, COUNT(*) as count
FROM pallet_movements
GROUP BY movement_type
ORDER BY movement_type;
