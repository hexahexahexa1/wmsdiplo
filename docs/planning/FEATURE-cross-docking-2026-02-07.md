# FEATURE: Cross-Docking End-to-End (Receipt-Centric)

## Overview
- Goal: deliver complete cross-docking flow from receiving to final shipping with partial shipping support.
- User story: as `ADMIN/SUPERVISOR`, I can move a cross-dock receipt through `READY_FOR_SHIPMENT -> PLACING -> READY_FOR_SHIPMENT -> SHIPPING_IN_PROGRESS -> SHIPPED`; as `OPERATOR`, I execute assigned tasks in terminal.
- Scope: receipt-centric cross-docking only, no full outbound orders module.

## Technical Design
### Modules
- `shared-contracts`: extend import/manual receipt DTOs and analytics health DTO.
- `core-api`: statuses, workflow logic, controllers, DB migrations.
- `import-service`: parse `crossDock` and `outboundRef` from XML.
- `desktop-client`: new receipt actions and shipping task visibility in terminal.

### Domain & Data
- New receipt statuses:
  - `SHIPPING_IN_PROGRESS`
  - `SHIPPED`
- New task type:
  - `SHIPPING`
- Receipt field:
  - `outboundRef` (nullable)

### API
- `POST /api/receipts/{id}/start-shipping` -> `{count}`
- `POST /api/receipts/{id}/complete-shipping` -> `202 Accepted`
- `POST /api/tasks/{id}/scans` routes `TaskType.SHIPPING` to shipping workflow.
- Existing `POST /api/receipts/{id}/start-placement` now supports cross-dock receipts from `READY_FOR_SHIPMENT`.

### Workflow
- Receiving:
  - cross-dock receipt completes receiving into `READY_FOR_SHIPMENT`.
- Placement:
  - cross-dock may start placement from `READY_FOR_SHIPMENT`.
  - cross-dock complete placement returns to `READY_FOR_SHIPMENT` (not `STOCKED`).
- Shipping:
  - start only for cross-dock receipt in `READY_FOR_SHIPMENT`.
  - create one shipping task per pallet.
  - partial scans keep receipt in `SHIPPING_IN_PROGRESS`.
  - auto-complete to `SHIPPED` when all shipping tasks are `COMPLETED`.
  - manual `complete-shipping` is fallback and requires all shipping tasks completed.

## Step-by-step Implementation Plan
1. Contracts and model:
   - Add `crossDock`, `outboundRef` in import/manual flows.
   - Add `outboundRef` to receipt DTO.
2. Domain/workflow:
   - Add statuses and `TaskType.SHIPPING`.
   - Update placement rules for cross-dock.
   - Implement `ShippingWorkflowService`.
3. Controllers:
   - Add shipping endpoints to receipt controller.
   - Add shipping route in task scan controller.
4. Data:
   - Add `outbound_ref` migration and cross-dock/shipping indexes.
   - Sync `database/init_schema.sql` with updated enum/state model.
5. Desktop:
   - Add start/complete shipping actions in receipts pane.
   - Add outbound ref column.
   - Include shipping tasks in terminal task lists.
6. Analytics:
   - Extend receiving health with cross-dock stuck counters.
7. Verification:
   - Update tests for changed DTO constructors and controller dependencies.
   - Add/adjust scan routing and analytics expectations.

## Test Strategy
### Unit
- Shipping workflow: start state validation, task creation, scan updates, auto-completion.
- Placement workflow for cross-dock: transition back to `READY_FOR_SHIPMENT`.
- DTO mapping includes `outboundRef`.

### Integration
- Receipt endpoints:
  - `start-shipping` creates tasks and sets receipt status.
  - `complete-shipping` rejects when not all tasks completed.
- Task scans:
  - `TaskType.SHIPPING` requests are handled by shipping workflow branch.

### UI
- Receipts screen:
  - action availability by status and `crossDock`.
  - `outboundRef` column visibility.
- Terminal:
  - shipping tasks appear in My/All lists.

## Decisions Log
- Cross-docking control remains supervisor/admin-driven for phase transitions.
- Shipping granularity = one task per pallet.
- `outboundRef` remains optional string.
- Backward compatibility is preserved for non-cross-dock receipts.
