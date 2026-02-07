# FEATURE: Palletization by SKU Unit Configs (2026-02-07)

## Overview
### Goal
Introduce configurable palletization rules per SKU unit of measure (UOM): base UOM + conversion factors + `unitsPerPallet`.

### User Story
As a warehouse supervisor, I can configure units for each SKU and specify pallet capacity per unit, so receiving tasks are auto-split by real palletization rules.

### Scope
- Shared contracts for SKU unit configuration and task quantity context.
- Core API support for SKU unit configuration CRUD (replace strategy), strict line UOM validation, and receiving quantity conversion to base UOM.
- Desktop client support for editing SKU unit configs and showing line/base quantity context in terminal tasks.
- Schema alignment in `database/init_schema.sql`.

## Technical Design
### Data Model
- New entity/table: `sku_unit_configs`:
  - `sku_id`, `unit_code`, `factor_to_base`, `units_per_pallet`, `is_base`, `active`.
- `receipt_lines` snapshot fields:
  - `qty_expected_base`
  - `unit_factor_to_base`
  - `units_per_pallet_snapshot`

### Domain Rules
- Exactly one base unit per SKU.
- Base unit must be active and have `factorToBase = 1`.
- `factorToBase > 0`, `unitsPerPallet > 0`.
- Receipt line UOM must be configured and active for the SKU.
- Operational quantities (`task.qtyAssigned`, `task.qtyDone`, `pallet.quantity`, `scan.qty`) are persisted in base UOM.

### API Changes
- Added DTOs:
  - `SkuUnitConfigDto`
  - `UpsertSkuUnitConfigsRequest`
- Extended DTOs:
  - `TaskDto`: `lineUom`, `baseUom`, `unitFactorToBase`
  - `ReceiptLineDto`: snapshot fields above
- New endpoints:
  - `GET /api/skus/{id}/unit-configs`
  - `PUT /api/skus/{id}/unit-configs`

### Service Logic
- `SkuService`:
  - manages SKU unit config lifecycle and validation;
  - auto-creates default base config for legacy/new SKUs.
- `ReceiptService`:
  - validates line UOM via `SkuService.getActiveUnitConfigOrThrow`;
  - stores UOM snapshot fields on `ReceiptLine`.
- `ReceivingWorkflowService`:
  - splits receiving tasks by snapshot palletization in base UOM;
  - converts scanned quantity from line UOM to base UOM.

### Desktop Client
- SKU page:
  - added `Units & Palletization` action.
  - modal editor for unit code, factor, units/pallet, base flag, active flag.
- Terminal task card:
  - displays assigned quantity in line UOM and base UOM (when different).
  - quantity input label/prompt now indicates input UOM.

## Implementation Plan
1. Contracts and DTO extensions in `shared-contracts`.
2. Core API entity/repository/service/controller changes for unit configs.
3. Receipt snapshot validation and receiving conversion/split updates.
4. Desktop API integration and SKU configuration modal.
5. Localization keys for new UI labels/messages.
6. Schema update in `database/init_schema.sql`.

## Test Strategy
### Backend
- Compile and run `:core-api:test`.
- Validate existing workflow integration (`ReceivingImprovementsIT`) with scale-safe quantity assertions.
- Update unit tests impacted by constructor/DTO changes (`TaskMapperTest`, `ReceiptServiceTest`, `SkuServiceTest`).

### Desktop
- Compile check with `:desktop-client:compileJava`.
- Manual smoke checks:
  - open SKU modal, load/edit/save unit configs;
  - verify terminal quantity labels and task document quantity formatting.

## Migration Plan
- For fresh bootstrap schema: `database/init_schema.sql` already includes required structures.
- For existing DB:
  - add `sku_unit_configs` table;
  - add snapshot columns to `receipt_lines`;
  - backfill default base config per SKU from `skus.uom` and legacy capacity where available.

## Rollback Strategy
- Keep legacy `skus.pallet_capacity` as fallback source during transition.
- If unit-config update fails in production, revert UI access to config modal and rely on legacy behavior.
- Snapshot fields are additive and backward-compatible for reads.

## Decisions Log
- 2026-02-07: chose replace strategy for SKU unit configs (`PUT` full set) to simplify consistency checks.
- 2026-02-07: chose strict blocking for unknown/inactive line UOM in manual/import receipts.
- 2026-02-07: chose base-UOM persistence for all operational quantities to avoid mixed-unit arithmetic.
