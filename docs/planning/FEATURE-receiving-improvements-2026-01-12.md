# Feature Plan: Receiving Workflow Improvements

**Date**: 2026-01-12  
**Author**: AI Assistant (based on analytics.txt)  
**Status**: Draft  
**Related Issues**: N/A

---

## 1. Overview

### Business Goal
Устранить критические недостатки текущего процесса приходования товара на склад и добавить функциональность для повышения точности учёта, гибкости обработки крупных поставок, и улучшения аналитики операций.

### Scope
Реализовать 7 групп улучшений процесса приходования:
1. ✅ UNDER_QTY Detection - автоматическая детекция недопоставок
2. ✅ Damaged Goods Tracking - учёт повреждённого товара
3. ✅ Lot & Expiry Date Management - управление партиями и сроками годности
4. ✅ Multi-Pallet Line Support - поддержка многопаллетных строк
5. ✅ Cross-Docking Support - поддержка кросс-доккинга
6. ✅ Task Prioritization - приоритизация задач
7. ✅ Analytics & Metrics - аналитика и метрики
8. ✅ Bulk Operations - массовые операции (назначение задач, создание паллет, изменение приоритета)

### User Stories

**As an operator**, I want the system to automatically detect under-deliveries so that all discrepancies are properly recorded without manual intervention.

**As an operator**, I want to mark damaged goods during receiving with damage types and descriptions so that we can track quality issues and file claims.

**As a warehouse manager**, I want to track lot numbers and expiry dates so that we can implement FEFO strategy and recall batches if needed.

**As a supervisor**, I want to split large receipt lines into multiple tasks so that multiple operators can work in parallel on big deliveries.

**As a supervisor**, I want to mark urgent receipts as cross-dock so that goods bypass storage and go directly to shipping.

**As a supervisor**, I want to prioritize tasks so that urgent deliveries are processed first.

**As an admin**, I want to see analytics dashboards and export reports so that I can monitor receiving performance and identify issues.

**As a supervisor**, I want to bulk-assign tasks to operators and bulk-create pallets so that I can manage work efficiently.

### Acceptance Criteria

**UNDER_QTY Detection:**
- [ ] System automatically creates Discrepancy of type UNDER_QTY when task is completed with qtyDone < qtyExpected
- [ ] Discrepancy is auto-resolved (resolved=true)
- [ ] qtyActual is recorded correctly in Discrepancy

**Damaged Goods Tracking:**
- [ ] Operator can mark scan with damageFlag, damageType, damageDescription
- [ ] System creates Discrepancy of type DAMAGE
- [ ] Pallet status is set to DAMAGED
- [ ] Damaged pallets are placed in DAMAGED location type cells
- [ ] Supported damage types: PHYSICAL_DAMAGE, WATER_DAMAGE, EXPIRED, TEMPERATURE_ABUSE, CONTAMINATION, OTHER

**Lot & Expiry Date Management:**
- [ ] Operator can specify lotNumber and expiryDate in scan request
- [ ] Pallet stores lotNumber and expiryDate
- [ ] System validates expiryDate < currentDate → creates Discrepancy EXPIRED_PRODUCT
- [ ] System validates lotNumber mismatch → creates Discrepancy LOT_MISMATCH
- [ ] ReceiptLine can specify lotNumberExpected for validation

**Multi-Pallet Line Support:**
- [ ] SKU has palletCapacity field (nullable)
- [ ] If qtyExpected > palletCapacity, system auto-creates N tasks during startReceiving()
- [ ] Each task is labeled "Паллета X из строки Y"
- [ ] Tasks are independent and can be assigned to different operators

**Cross-Docking Support:**
- [ ] Receipt has crossDock flag (boolean, default false)
- [ ] If crossDock=true, after ACCEPTED → status becomes READY_FOR_SHIPMENT
- [ ] System creates PLACEMENT tasks with target location type CROSS_DOCK
- [ ] Pallets are moved to CROSS_DOCK zone instead of regular STORAGE

**Task Prioritization:**
- [ ] Task priority field is used (1-999, default 100)
- [ ] Supervisor can change task priority via API
- [ ] Desktop client sorts tasks by priority DESC, createdAt ASC
- [ ] Bulk priority change is available

**Analytics & Metrics:**
- [ ] Dashboard shows receiving performance metrics (avg time, receipt counts by status)
- [ ] Dashboard shows discrepancy analytics (count by type, percentage)
- [ ] Dashboard shows pallet analytics (count by status, damaged rate)
- [ ] Data is calculated on-the-fly from existing tables
- [ ] Export to CSV is available
- [ ] Only accessible to admin and supervisor roles

**Bulk Operations:**
- [ ] Bulk task assignment API: POST /api/tasks/bulk-assign
- [ ] Bulk pallet creation API: POST /api/pallets/bulk-create (PLT-XXX to PLT-XXX+N)
- [ ] Bulk priority change API: POST /api/tasks/bulk-set-priority
- [ ] Desktop client UI: checkboxes for tasks, "Назначить выбранные" and "Изменить приоритет" buttons
- [ ] Errors handled with best-effort (return list of successes and failures)

---

## 2. Functional Requirements

### Inputs

**RecordScanRequest (enhanced):**
```java
public record RecordScanRequest(
    // Existing fields
    String palletCode,
    String sscc,
    String barcode,
    Integer qty,
    String deviceId,
    String comment,
    String locationCode,
    
    // NEW FIELDS
    Boolean damageFlag,           // Flag for damaged goods
    String damageType,            // Enum: PHYSICAL_DAMAGE, WATER_DAMAGE, EXPIRED, TEMPERATURE_ABUSE, CONTAMINATION, OTHER
    String damageDescription,     // Free text description
    String lotNumber,             // Lot/batch number
    LocalDate expiryDate          // Expiry date
) {}
```

**BulkAssignRequest:**
```java
public record BulkAssignRequest(
    List<Long> taskIds,
    String assignee
) {}
```

**BulkCreatePalletsRequest:**
```java
public record BulkCreatePalletsRequest(
    Integer startNumber,   // e.g., 100 → PLT-100
    Integer count          // e.g., 50 → creates PLT-100...PLT-149
) {}
```

**BulkSetPriorityRequest:**
```java
public record BulkSetPriorityRequest(
    List<Long> taskIds,
    Integer priority
) {}
```

### Outputs

**TaskDto (enhanced with priority)**

**DiscrepancyDto (enhanced with new types)**

**AnalyticsDto:**
```java
public record ReceivingAnalyticsDto(
    LocalDate fromDate,
    LocalDate toDate,
    
    // Receiving Performance
    Double avgReceivingTimeHours,
    Map<String, Integer> receiptsByStatus,  // {DRAFT: 5, CONFIRMED: 10, ...}
    
    // Discrepancy Analytics
    Map<String, Integer> discrepanciesByType,  // {UNDER_QTY: 15, DAMAGE: 8, ...}
    Double discrepancyRate,  // % receipts with discrepancies
    
    // Pallet Analytics
    Map<String, Integer> palletsByStatus,  // {PLACED: 120, DAMAGED: 5, ...}
    Double damagedPalletsRate
) {}
```

### Business Rules

1. **UNDER_QTY Creation**: When task is completed with qtyDone < qtyExpected, automatically create Discrepancy with:
   - type = "UNDER_QTY"
   - qtyExpected = task.qtyAssigned
   - qtyActual = task.qtyDone
   - resolved = true (auto-resolved)

2. **Damaged Goods Handling**: When scan has damageFlag=true:
   - Create Discrepancy type="DAMAGE"
   - Set pallet.status = DAMAGED
   - Target location must be type DAMAGED (not regular STORAGE)

3. **Lot Number Validation**: If ReceiptLine.lotNumberExpected is set and scan.lotNumber doesn't match → create LOT_MISMATCH discrepancy (but still accept)

4. **Expiry Date Validation**: If scan.expiryDate < currentDate → create EXPIRED_PRODUCT discrepancy (but still accept)

5. **Multi-Pallet Auto-Split**: During startReceiving(), for each ReceiptLine:
   - If SKU.palletCapacity is null → create 1 task (current behavior)
   - If qtyExpected <= palletCapacity → create 1 task
   - If qtyExpected > palletCapacity → create N tasks where N = ceil(qtyExpected / palletCapacity)
     - Each task: description = "Паллета X из строки {lineNo}"

6. **Cross-Dock Workflow**: If Receipt.crossDock=true:
   - After completeReceiving() → receipt.status = READY_FOR_SHIPMENT (skip ACCEPTED)
   - PlacementWorkflowService creates tasks with target LocationType.CROSS_DOCK

7. **Task Priority Sorting**: Tasks are retrieved ordered by:
   - priority DESC (higher first)
   - createdAt ASC (older first)

8. **Bulk Operations Error Handling**: Best-effort approach:
   - Process each item independently
   - Return: `{ successes: [...], failures: [{id, error}, ...] }`

### Edge Cases

| Scenario | Handling |
|----------|----------|
| UNDER_QTY with qtyDone=0 | Create discrepancy, allow task completion |
| Damaged scan on existing pallet | Error - cannot mark already-received pallet as damaged |
| Expiry date is null | Allow - lot tracking is optional |
| Lot number mismatch but lotNumberExpected is null | No validation, accept as-is |
| Cross-dock receipt with no CROSS_DOCK locations | Error during startPlacement() |
| Bulk assign task already assigned to another operator | Override assignment (with warning in failures list) |
| Bulk create pallets with duplicate PLT codes | Skip duplicates, return in failures list |
| Priority value outside 1-999 range | Clamp to range (1-999) |

---

## 3. Technical Design

### Affected Modules

- [x] **core-api** - main business logic changes
- [x] **shared-contracts** - new/enhanced DTOs
- [ ] **import-service** - no changes needed
- [x] **desktop-client** - UI for bulk operations, analytics dashboard

### Domain Model Changes

#### New Entities

None (using existing entities with enhancements)

#### Modified Entities

**Sku** (core-api/src/main/java/com/wmsdipl/core/domain/Sku.java):
```java
@Column(name = "pallet_capacity", precision = 10, scale = 2)
private BigDecimal palletCapacity;  // NEW - standard pallet quantity
```

**Receipt** (core-api/src/main/java/com/wmsdipl/core/domain/Receipt.java):
```java
@Column(name = "cross_dock")
private Boolean crossDock = false;  // NEW - cross-dock flag
```

**ReceiptLine** (core-api/src/main/java/com/wmsdipl/core/domain/ReceiptLine.java):
```java
@Column(name = "lot_number_expected")
private String lotNumberExpected;  // NEW - expected lot number from supplier

@Column(name = "expiry_date_expected")
private LocalDate expiryDateExpected;  // NEW - expected expiry date
```

**Scan** (core-api/src/main/java/com/wmsdipl/core/domain/Scan.java):
```java
@Column(name = "damage_flag")
private Boolean damageFlag = false;  // NEW - is damaged

@Column(name = "damage_type", length = 64)
private String damageType;  // NEW - PHYSICAL_DAMAGE, WATER_DAMAGE, etc.

@Column(name = "damage_description", length = 512)
private String damageDescription;  // NEW - free text

@Column(name = "lot_number")
private String lotNumber;  // NEW - scanned lot number

@Column(name = "expiry_date")
private LocalDate expiryDate;  // NEW - scanned expiry date
```

**LocationType** (core-api/src/main/java/com/wmsdipl/core/domain/LocationType.java):
```java
public enum LocationType {
    RECEIVING,
    STORAGE,
    SHIPPING,
    CROSS_DOCK,  // NEW
    DAMAGED      // NEW
}
```

**PalletStatus** (core-api/src/main/java/com/wmsdipl/core/domain/PalletStatus.java):
```java
public enum PalletStatus {
    EMPTY,
    RECEIVING,
    RECEIVED,
    PLACING,
    PLACED,
    PICKED,
    DAMAGED,     // NEW - damaged goods
    QUARANTINE   // NEW - quarantine
}
```

#### Database Migrations

**V022__receiving_improvements.sql:**
```sql
-- 1. Add palletCapacity to SKUs
ALTER TABLE skus 
ADD COLUMN pallet_capacity NUMERIC(10,2);

COMMENT ON COLUMN skus.pallet_capacity IS 'Standard pallet quantity for auto-splitting large receipt lines';

-- 2. Add cross-dock flag to Receipts
ALTER TABLE receipts 
ADD COLUMN cross_dock BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN receipts.cross_dock IS 'True if receipt should bypass storage (cross-docking)';

-- 3. Add lot tracking to ReceiptLines
ALTER TABLE receipt_lines 
ADD COLUMN lot_number_expected VARCHAR(128),
ADD COLUMN expiry_date_expected DATE;

COMMENT ON COLUMN receipt_lines.lot_number_expected IS 'Expected lot number from supplier document';

-- 4. Add damage tracking to Scans
ALTER TABLE scans 
ADD COLUMN damage_flag BOOLEAN DEFAULT FALSE,
ADD COLUMN damage_type VARCHAR(64),
ADD COLUMN damage_description VARCHAR(512),
ADD COLUMN lot_number VARCHAR(128),
ADD COLUMN expiry_date DATE;

COMMENT ON COLUMN scans.damage_flag IS 'True if goods are damaged';
COMMENT ON COLUMN scans.damage_type IS 'Damage type: PHYSICAL_DAMAGE, WATER_DAMAGE, EXPIRED, TEMPERATURE_ABUSE, CONTAMINATION, OTHER';

-- 5. Add indexes for performance
CREATE INDEX idx_scans_damage_flag ON scans(damage_flag) WHERE damage_flag = TRUE;
CREATE INDEX idx_scans_lot_number ON scans(lot_number);
CREATE INDEX idx_pallets_lot_number ON pallets(lot_number);
CREATE INDEX idx_tasks_priority ON tasks(priority DESC, created_at ASC);
```

**V023__new_location_types.sql:**
```sql
-- Note: LocationType is enum in Java, but VARCHAR in DB
-- Ensure existing values are intact, no migration needed
-- New values CROSS_DOCK and DAMAGED will be accepted automatically

-- Add comment for clarity
COMMENT ON COLUMN locations.location_type IS 'RECEIVING, STORAGE, SHIPPING, CROSS_DOCK, DAMAGED';
```

### API Design

#### New Endpoints

**UNDER_QTY Detection** (automatic - no new endpoint needed)

**Damaged Goods Tracking** (enhanced existing RecordScanRequest)

**Lot Management** (enhanced existing RecordScanRequest)

**Multi-Pallet Support** (automatic during startReceiving)

**Cross-Docking:**
```http
POST /api/receipts
{
  "docNumber": "RCP-2024-001",
  "supplier": "Supplier A",
  "crossDock": true,  // NEW FIELD
  "lines": [...]
}
```

**Task Prioritization:**
```http
POST /api/tasks/{id}/set-priority
Content-Type: application/json

{
  "priority": 900
}

Response: 200 OK
```

**Bulk Task Assignment:**
```http
POST /api/tasks/bulk-assign
Content-Type: application/json

{
  "taskIds": [1, 2, 3, 4],
  "assignee": "operator1"
}

Response: 200 OK
{
  "successes": [1, 2, 3],
  "failures": [
    {"id": 4, "error": "Task already completed"}
  ]
}
```

**Bulk Pallet Creation:**
```http
POST /api/pallets/bulk-create
Content-Type: application/json

{
  "startNumber": 100,
  "count": 50
}

Response: 201 Created
{
  "created": ["PLT-100", "PLT-101", ..., "PLT-149"],
  "failures": []
}
```

**Bulk Priority Change:**
```http
POST /api/tasks/bulk-set-priority
Content-Type: application/json

{
  "taskIds": [1, 2, 3],
  "priority": 500
}

Response: 200 OK
{
  "successes": [1, 2, 3],
  "failures": []
}
```

**Analytics Dashboard:**
```http
GET /api/analytics/receiving-performance?from=2026-01-01&to=2026-01-31

Response: 200 OK
{
  "fromDate": "2026-01-01",
  "toDate": "2026-01-31",
  "avgReceivingTimeHours": 2.5,
  "receiptsByStatus": {
    "DRAFT": 5,
    "CONFIRMED": 10,
    "ACCEPTED": 45,
    "STOCKED": 120
  },
  "discrepanciesByType": {
    "UNDER_QTY": 15,
    "DAMAGE": 8,
    "OVER_QTY": 2,
    "LOT_MISMATCH": 3,
    "EXPIRED_PRODUCT": 1
  },
  "discrepancyRate": 12.5,
  "palletsByStatus": {
    "PLACED": 450,
    "DAMAGED": 12,
    "QUARANTINE": 3
  },
  "damagedPalletsRate": 2.7
}
```

**Analytics Export:**
```http
GET /api/analytics/export-csv?from=2026-01-01&to=2026-01-31

Response: 200 OK
Content-Type: text/csv
Content-Disposition: attachment; filename="receiving-analytics-2026-01.csv"

<CSV data>
```

#### Request DTOs

All defined in **shared-contracts** module:

```java
// Enhanced existing
public record RecordScanRequest(
    String palletCode,
    String sscc,
    String barcode,
    Integer qty,
    String deviceId,
    String comment,
    String locationCode,
    Boolean damageFlag,
    String damageType,
    String damageDescription,
    String lotNumber,
    LocalDate expiryDate
) {}

// New
public record SetPriorityRequest(Integer priority) {}

public record BulkAssignRequest(List<Long> taskIds, String assignee) {}

public record BulkCreatePalletsRequest(Integer startNumber, Integer count) {}

public record BulkSetPriorityRequest(List<Long> taskIds, Integer priority) {}
```

#### Response DTOs

```java
public record BulkOperationResult<T>(
    List<T> successes,
    List<BulkOperationFailure> failures
) {}

public record BulkOperationFailure(Long id, String error) {}

public record ReceivingAnalyticsDto(
    LocalDate fromDate,
    LocalDate toDate,
    Double avgReceivingTimeHours,
    Map<String, Integer> receiptsByStatus,
    Map<String, Integer> discrepanciesByType,
    Double discrepancyRate,
    Map<String, Integer> palletsByStatus,
    Double damagedPalletsRate
) {}

public record PalletCreationResult(
    List<String> created,
    List<BulkOperationFailure> failures
) {}
```

### Service Layer Design

#### New Services

**AnalyticsService** (core-api/src/main/java/com/wmsdipl/core/service/AnalyticsService.java):
```java
@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    
    public ReceivingAnalyticsDto getReceivingPerformance(LocalDate from, LocalDate to) {
        // Calculate metrics from receipts, tasks, discrepancies, pallets
    }
    
    public byte[] exportToCsv(LocalDate from, LocalDate to) {
        // Generate CSV export
    }
}
```

**BulkOperationsService** (core-api/src/main/java/com/wmsdipl/core/service/BulkOperationsService.java):
```java
@Service
@Transactional
public class BulkOperationsService {
    
    public BulkOperationResult<Long> bulkAssignTasks(BulkAssignRequest request) {
        // Best-effort assignment
    }
    
    public PalletCreationResult bulkCreatePallets(BulkCreatePalletsRequest request) {
        // Generate PLT-XXX codes
    }
    
    public BulkOperationResult<Long> bulkSetPriority(BulkSetPriorityRequest request) {
        // Best-effort priority change
    }
}
```

#### Modified Services

**ReceivingWorkflowService**:
- `startReceiving()` - add multi-pallet auto-split logic
- `recordScan()` - add damage tracking, lot tracking, expiry validation
- `completeReceiving()` - add cross-dock handling

**TaskLifecycleService**:
- `complete()` - add UNDER_QTY detection logic

**PlacementWorkflowService**:
- `startPlacement()` - handle cross-dock receipts (target CROSS_DOCK locations)
- `recordPlacement()` - handle damaged pallets (target DAMAGED locations)

**TaskService** (new):
```java
@Service
@Transactional
public class TaskService {
    
    public void updatePriority(Long taskId, Integer priority) {
        // Clamp to 1-999 range
    }
    
    public List<Task> findTasksSorted(TaskStatus status) {
        // Order by priority DESC, createdAt ASC
    }
}
```

#### Service Dependencies

```
ReceivingWorkflowService
  ├── ReceiptRepository
  ├── TaskRepository
  ├── ScanRepository
  ├── DiscrepancyRepository
  ├── PalletRepository
  ├── SkuRepository (NEW - for palletCapacity)
  ├── LocationRepository
  └── StockMovementService

TaskLifecycleService
  ├── TaskRepository
  ├── DiscrepancyRepository (NEW - for UNDER_QTY)
  └── StockMovementService

AnalyticsService
  ├── ReceiptRepository
  ├── TaskRepository
  ├── DiscrepancyRepository
  └── PalletRepository

BulkOperationsService
  ├── TaskRepository
  ├── PalletRepository
  └── TaskLifecycleService
```

### Workflow & State Machine

**Cross-Dock Workflow:**
```
[DRAFT] --confirm--> [CONFIRMED] --start--> [IN_PROGRESS] 
    --complete--> [READY_FOR_SHIPMENT]
                           ↓
                    [PLACEMENT to CROSS_DOCK zone]
                           ↓
                       [SHIPPED]
```

**Normal Workflow (unchanged):**
```
[DRAFT] --> [CONFIRMED] --> [IN_PROGRESS] --> [ACCEPTED] 
    --> [PLACING] --> [STOCKED]
```

**Damaged Pallet Workflow:**
```
[RECEIVING] --scan with damageFlag=true--> [DAMAGED]
    --> [PLACEMENT to DAMAGED zone]
    --> [PLACED in DAMAGED cell]
```

---

## 4. Error Handling

### Exception Mapping

| Exception | HTTP Status | Scenario |
|-----------|-------------|----------|
| `IllegalArgumentException` | 400 | Invalid priority value, invalid damage type |
| `ResponseStatusException(404)` | 404 | Task not found, Receipt not found |
| `ResponseStatusException(400)` | 400 | No CROSS_DOCK locations available |
| `ResponseStatusException(400)` | 400 | No DAMAGED locations available |
| `ResponseStatusException(409)` | 409 | Duplicate pallet code in bulk-create |

### Error Response Format

Standard Spring Boot error response:
```json
{
  "timestamp": "2026-01-12T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "No CROSS_DOCK locations available",
  "path": "/api/placements/start"
}
```

Bulk operations return partial success:
```json
{
  "successes": [1, 2, 3],
  "failures": [
    {"id": 4, "error": "Task already completed"},
    {"id": 5, "error": "Task not found"}
  ]
}
```

---

## 5. Testing Strategy

### Unit Tests

**TaskLifecycleServiceTest**:
- `shouldCreateUnderQtyDiscrepancy_WhenQtyDoneLessThanExpected()`
- `shouldAutoResolveUnderQtyDiscrepancy()`
- `shouldNotCreateDiscrepancy_WhenQtyDoneEqualsExpected()`

**ReceivingWorkflowServiceTest**:
- `shouldCreateMultipleTasks_WhenQtyExceedsPalletCapacity()`
- `shouldCreateDamageDiscrepancy_WhenDamageFlagTrue()`
- `shouldSetPalletStatusDamaged_WhenDamageFlagTrue()`
- `shouldCreateExpiredDiscrepancy_WhenExpiryDatePast()`
- `shouldCreateLotMismatchDiscrepancy_WhenLotNumberDoesNotMatch()`
- `shouldSetStatusReadyForShipment_WhenCrossDockTrue()`

**PlacementWorkflowServiceTest**:
- `shouldTargetCrossDockLocation_WhenReceiptCrossDock()`
- `shouldTargetDamagedLocation_WhenPalletStatusDamaged()`

**TaskServiceTest**:
- `shouldUpdatePriority_WhenValidValue()`
- `shouldClampPriority_WhenOutOfRange()`
- `shouldSortTasksByPriorityAndCreatedAt()`

**BulkOperationsServiceTest**:
- `shouldBulkAssignTasks_WhenAllValid()`
- `shouldReturnPartialSuccess_WhenSomeTasksInvalid()`
- `shouldBulkCreatePallets_WithSequentialNumbers()`
- `shouldSkipDuplicates_InBulkPalletCreation()`
- `shouldBulkSetPriority_WhenAllValid()`

**AnalyticsServiceTest**:
- `shouldCalculateReceivingMetrics_ForDateRange()`
- `shouldCalculateDiscrepancyRate_Correctly()`
- `shouldExportCsv_WithCorrectFormat()`

### Integration Tests

**ReceivingWorkflowIntegrationTest**:
- `shouldCompleteFullReceivingFlow_WithUnderQty()`
- `shouldCompleteReceivingFlow_WithDamagedGoods()`
- `shouldCompleteReceivingFlow_WithLotTracking()`
- `shouldCompleteMultiPalletReceiving_WithAutoSplit()`
- `shouldCompleteCrossDockFlow_EndToEnd()`

**TaskControllerTest** (MockMvc):
- `POST /api/tasks/{id}/set-priority - shouldReturn200`
- `POST /api/tasks/bulk-assign - shouldReturn200WithPartialSuccess`
- `POST /api/tasks/bulk-set-priority - shouldReturn200`

**PalletControllerTest** (MockMvc):
- `POST /api/pallets/bulk-create - shouldReturn201WithCreatedPallets`
- `POST /api/pallets/bulk-create - shouldHandleDuplicates`

**AnalyticsControllerTest** (MockMvc):
- `GET /api/analytics/receiving-performance - shouldReturn200WithMetrics`
- `GET /api/analytics/export-csv - shouldReturnCsvFile`

### Test Data

**Sample Damaged Scan:**
```json
{
  "palletCode": "PLT-100",
  "barcode": "1234567890123",
  "qty": 100,
  "damageFlag": true,
  "damageType": "WATER_DAMAGE",
  "damageDescription": "Упаковка промокла во время транспортировки"
}
```

**Sample Lot Tracking Scan:**
```json
{
  "palletCode": "PLT-101",
  "barcode": "1234567890123",
  "qty": 500,
  "lotNumber": "LOT-2026-001",
  "expiryDate": "2027-12-31"
}
```

**Sample Cross-Dock Receipt:**
```json
{
  "docNumber": "RCP-URGENT-001",
  "supplier": "Express Supplier",
  "crossDock": true,
  "priority": 900,
  "lines": [...]
}
```

---

## 6. Implementation Checklist

### Phase 1: Data Model & Migrations (Week 1)

- [ ] **V022__receiving_improvements.sql**
  - [ ] Add pallet_capacity to skus
  - [ ] Add cross_dock to receipts
  - [ ] Add lot_number_expected, expiry_date_expected to receipt_lines
  - [ ] Add damage_flag, damage_type, damage_description, lot_number, expiry_date to scans
  - [ ] Add indexes (scans damage_flag, lot_number; tasks priority)
  
- [ ] **V023__new_location_types.sql**
  - [ ] Add comments for CROSS_DOCK and DAMAGED location types

- [ ] **Update domain entities**
  - [ ] Sku.java - add palletCapacity field
  - [ ] Receipt.java - add crossDock field
  - [ ] ReceiptLine.java - add lotNumberExpected, expiryDateExpected
  - [ ] Scan.java - add damageFlag, damageType, damageDescription, lotNumber, expiryDate
  - [ ] LocationType.java - add CROSS_DOCK, DAMAGED enum values
  - [ ] PalletStatus.java - add DAMAGED, QUARANTINE enum values

- [ ] **Run migrations and test**
  - [ ] Verify migrations apply cleanly
  - [ ] Insert test data with new fields

### Phase 2: DTOs & Contracts (Week 1)

- [ ] **shared-contracts module**
  - [ ] Enhance RecordScanRequest with new fields
  - [ ] Create SetPriorityRequest
  - [ ] Create BulkAssignRequest
  - [ ] Create BulkCreatePalletsRequest
  - [ ] Create BulkSetPriorityRequest
  - [ ] Create BulkOperationResult<T>
  - [ ] Create BulkOperationFailure
  - [ ] Create ReceivingAnalyticsDto
  - [ ] Create PalletCreationResult

- [ ] **core-api mappers**
  - [ ] Update ReceiptMapper for crossDock field
  - [ ] Update ScanMapper for damage and lot fields
  - [ ] Create AnalyticsMapper (if needed)

### Phase 3: Service Layer - Critical Features (Week 2)

- [ ] **TaskLifecycleService**
  - [ ] Modify complete() to detect UNDER_QTY
  - [ ] Create Discrepancy when qtyDone < qtyExpected
  - [ ] Auto-resolve UNDER_QTY discrepancies
  - [ ] Write unit tests

- [ ] **ReceivingWorkflowService**
  - [ ] Modify startReceiving() for multi-pallet auto-split
  - [ ] Add logic to check SKU.palletCapacity
  - [ ] Create N tasks when qtyExpected > palletCapacity
  - [ ] Modify recordScan() for damage tracking
  - [ ] Create DAMAGE discrepancy when damageFlag=true
  - [ ] Set pallet status to DAMAGED
  - [ ] Add lot number and expiry date handling
  - [ ] Validate expiryDate < currentDate → EXPIRED_PRODUCT
  - [ ] Validate lotNumber mismatch → LOT_MISMATCH
  - [ ] Modify completeReceiving() for cross-dock
  - [ ] Set status READY_FOR_SHIPMENT if crossDock=true
  - [ ] Write unit tests

- [ ] **PlacementWorkflowService**
  - [ ] Modify startPlacement() to handle cross-dock receipts
  - [ ] Target CROSS_DOCK location type for cross-dock
  - [ ] Modify recordPlacement() to handle damaged pallets
  - [ ] Target DAMAGED location type for DAMAGED pallets
  - [ ] Write unit tests

### Phase 4: Service Layer - Enhancements (Week 2-3)

- [ ] **TaskService** (new)
  - [ ] Implement updatePriority()
  - [ ] Clamp priority to 1-999 range
  - [ ] Implement findTasksSorted() with ORDER BY priority DESC, createdAt ASC
  - [ ] Write unit tests

- [ ] **BulkOperationsService** (new)
  - [ ] Implement bulkAssignTasks()
  - [ ] Best-effort approach, return successes/failures
  - [ ] Implement bulkCreatePallets()
  - [ ] Generate PLT-{startNumber} to PLT-{startNumber+count-1}
  - [ ] Skip duplicates, return failures
  - [ ] Implement bulkSetPriority()
  - [ ] Write unit tests

- [ ] **AnalyticsService** (new)
  - [ ] Implement getReceivingPerformance()
  - [ ] Calculate avgReceivingTimeHours from receipts
  - [ ] Count receipts by status
  - [ ] Count discrepancies by type
  - [ ] Calculate discrepancy rate
  - [ ] Count pallets by status
  - [ ] Calculate damaged pallets rate
  - [ ] Implement exportToCsv()
  - [ ] Generate CSV with proper headers
  - [ ] Write unit tests

### Phase 5: API Layer (Week 3)

- [ ] **TaskController**
  - [ ] POST /api/tasks/{id}/set-priority
  - [ ] POST /api/tasks/bulk-assign
  - [ ] POST /api/tasks/bulk-set-priority
  - [ ] Add Swagger annotations
  - [ ] Write MockMvc tests

- [ ] **PalletController**
  - [ ] POST /api/pallets/bulk-create
  - [ ] Add Swagger annotations
  - [ ] Write MockMvc tests

- [ ] **AnalyticsController** (new)
  - [ ] GET /api/analytics/receiving-performance
  - [ ] GET /api/analytics/export-csv
  - [ ] Add authorization (admin/supervisor only)
  - [ ] Add Swagger annotations
  - [ ] Write MockMvc tests

- [ ] **ReceiptController**
  - [ ] Update POST /api/receipts to accept crossDock field
  - [ ] Update Swagger annotations

### Phase 6: Desktop Client UI (Week 4)

- [ ] **Tasks Screen (Задания)**
  - [ ] Add checkboxes for task selection
  - [ ] Add "Назначить выбранные" button
  - [ ] Show dialog to select operator
  - [ ] Add "Изменить приоритет" button
  - [ ] Show dialog to enter new priority
  - [ ] Display priority column in task table
  - [ ] Sort by priority DESC, createdAt ASC

- [ ] **Pallets Screen (Паллеты)**
  - [ ] Add "Массовое создание" button
  - [ ] Show dialog with startNumber and count inputs
  - [ ] Display creation results (successes/failures)

- [ ] **Analytics Screen (Аналитика)** (new)
  - [ ] Create new tab accessible only to admin/supervisor
  - [ ] Add date range picker (from/to)
  - [ ] Display receiving performance metrics
  - [ ] Display discrepancy analytics (charts)
  - [ ] Display pallet analytics
  - [ ] Add "Экспорт в CSV" button
  - [ ] Download CSV file

- [ ] **Receiving Terminal**
  - [ ] Add damage tracking fields (damageFlag, damageType, damageDescription)
  - [ ] Add lot tracking fields (lotNumber, expiryDate)
  - [ ] Show validation errors for expired products

### Phase 7: Integration Testing (Week 4)

- [ ] **End-to-End Scenarios**
  - [ ] Complete receiving flow with UNDER_QTY
  - [ ] Complete receiving flow with damaged goods
  - [ ] Complete receiving flow with lot tracking
  - [ ] Complete multi-pallet receiving with auto-split
  - [ ] Complete cross-dock flow
  - [ ] Bulk assign tasks
  - [ ] Bulk create pallets
  - [ ] View analytics dashboard
  - [ ] Export analytics to CSV

- [ ] **Testcontainers Tests**
  - [ ] Write integration tests with real PostgreSQL
  - [ ] Test migrations V022, V023
  - [ ] Test full workflow scenarios

### Phase 8: Documentation (Week 5)

- [ ] Update README.md
  - [ ] Document new API endpoints
  - [ ] Document new features
  
- [ ] Update ER diagram (docs/er-diagram.mmd)
  - [ ] Add new fields to Sku, Receipt, ReceiptLine, Scan
  - [ ] Add new LocationType values
  - [ ] Add new PalletStatus values

- [ ] Update PROCESS_WORKFLOW.md
  - [ ] Document cross-dock workflow
  - [ ] Document damaged goods workflow
  - [ ] Document multi-pallet workflow

- [ ] Create user guide
  - [ ] How to use bulk operations
  - [ ] How to mark damaged goods
  - [ ] How to track lot numbers
  - [ ] How to view analytics

---

## 7. Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Multi-pallet auto-split creates too many tasks for large orders | High | Medium | Add configuration parameter max_tasks_per_line (default 10) |
| Analytics queries are slow on large datasets | Medium | High | Add database indexes, consider materialized views if needed |
| CROSS_DOCK location type not configured | High | Medium | Add validation during Receipt.confirm() to check location availability |
| DAMAGED location type not configured | High | Medium | Add validation during recordScan() to check location availability |
| Bulk operations timeout on large batches | Medium | Low | Implement pagination (max 100 items per batch) |
| Expiry date validation too strict | Low | Medium | Make validation configurable (warn vs error) |
| Desktop client performance degrades with analytics charts | Medium | Low | Implement lazy loading, limit data range to 90 days |

---

## 8. Performance Considerations

**Database Indexes:**
```sql
CREATE INDEX idx_scans_damage_flag ON scans(damage_flag) WHERE damage_flag = TRUE;
CREATE INDEX idx_scans_lot_number ON scans(lot_number);
CREATE INDEX idx_pallets_lot_number ON pallets(lot_number);
CREATE INDEX idx_tasks_priority ON tasks(priority DESC, created_at ASC);
CREATE INDEX idx_receipts_status_dates ON receipts(status, created_at, closed_at);
CREATE INDEX idx_discrepancies_type ON discrepancies(type);
```

**Query Optimization:**
- Use JOIN FETCH for analytics queries to avoid N+1
- Limit analytics date range to prevent full table scans
- Consider caching analytics results for 5 minutes (if real-time not critical)

**Pagination:**
- Analytics export: stream results to CSV instead of loading all into memory
- Bulk operations: enforce max 100 items per batch

---

## 9. Security Considerations

**Authorization:**
- Analytics endpoints: require ROLE_ADMIN or ROLE_SUPERVISOR
- Bulk operations: require ROLE_SUPERVISOR
- Task priority changes: require ROLE_SUPERVISOR
- Pallet bulk creation: require ROLE_SUPERVISOR or ROLE_ADMIN

**Validation:**
- Sanitize all input fields (damageDescription, lotNumber, etc.)
- Validate priority range (1-999)
- Validate date ranges (from <= to)
- Prevent SQL injection (use parameterized queries)

**Audit:**
- Log all bulk operations (who, when, what)
- Log task priority changes
- Log analytics exports

---

## 10. Questions & Decisions Log

### Open Questions
- [ ] **Max tasks per line**: What is the maximum number of tasks we should create for one receipt line? (proposed: 10)
- [ ] **Analytics retention**: How long to keep analytics data? (proposed: 2 years)
- [ ] **Photo upload**: If damage photos needed in future, what storage? (deferred)

### Decisions Made

**Decision**: UNDER_QTY discrepancies are auto-resolved  
**Rationale**: User confirmed that under-delivery is a normal process, not an error  
**Date**: 2026-01-12

**Decision**: No Rejection Workflow (item 10)  
**Rationale**: User prefers to handle write-offs separately as a future feature  
**Date**: 2026-01-12

**Decision**: Damaged pallets go to DAMAGED location type cells, not rejected  
**Rationale**: All goods are accepted, damaged ones are placed in special zones for later write-off  
**Date**: 2026-01-12

**Decision**: Use SKU.palletCapacity for auto-splitting  
**Rationale**: Simpler than using packagings table, sufficient for current needs  
**Date**: 2026-01-12

**Decision**: Cross-dock uses new LocationType.CROSS_DOCK  
**Rationale**: Clear separation from regular STORAGE and SHIPPING zones  
**Date**: 2026-01-12

**Decision**: Analytics calculated on-the-fly, not pre-aggregated  
**Rationale**: User prefers real-time data, dataset size is manageable  
**Date**: 2026-01-12

**Decision**: Bulk operations use best-effort error handling  
**Rationale**: Partial success is better than all-or-nothing for UX  
**Date**: 2026-01-12

---

## 11. Success Metrics

**Receiving Accuracy:**
- Discrepancy detection rate increases by 100% (all UNDER_QTY now captured)
- Damaged goods tracked: target >95% of damaged items recorded

**Operational Efficiency:**
- Multi-pallet receiving reduces time by 30% for large orders
- Cross-dock reduces lead time by 50% for urgent deliveries
- Bulk operations save 10 minutes per day for supervisors

**Visibility:**
- Analytics dashboard reduces ad-hoc queries by 80%
- Supervisor can export weekly reports in <2 minutes

---

## 12. Timeline Estimate

| Phase | Description | Duration | Dependencies |
|-------|-------------|----------|--------------|
| Phase 1 | Data Model & Migrations | 3 days | None |
| Phase 2 | DTOs & Contracts | 2 days | Phase 1 |
| Phase 3 | Service Layer - Critical | 5 days | Phase 2 |
| Phase 4 | Service Layer - Enhancements | 5 days | Phase 3 |
| Phase 5 | API Layer | 3 days | Phase 4 |
| Phase 6 | Desktop Client UI | 5 days | Phase 5 |
| Phase 7 | Integration Testing | 3 days | Phase 6 |
| Phase 8 | Documentation | 2 days | Phase 7 |
| **Total** | | **28 days (~5-6 weeks)** | |

**Breakdown by Priority:**
- Critical (Phases 1-3): 10 days (2 weeks)
- High Priority (Phase 4): 5 days (1 week)
- Medium/Low (Phases 5-8): 13 days (2.5 weeks)

---

## 13. References

**Related Documentation:**
- `docs/er-diagram.mmd` - Entity Relationship Diagram
- `PROCESS_WORKFLOW.md` - Current workflow documentation
- `AGENTS.md` - Development guidelines
- `docs/planning/analytics.txt` - Original analysis

**Similar Features:**
- `ReceivingWorkflowService.java:132-263` - recordScan() logic (to be enhanced)
- `TaskLifecycleService.java` - Task state management (to be enhanced)
- `PlacementWorkflowService.java` - Placement logic (to be enhanced)

**External Resources:**
- WMS Best Practices: Lot Tracking and Expiry Management
- Cross-Docking in Modern Warehouses
- Analytics Design Patterns for Operational Systems

---

## Appendix A: Damage Type Reference

```java
public enum DamageType {
    PHYSICAL_DAMAGE("Физическое повреждение упаковки"),
    WATER_DAMAGE("Намокание/влага"),
    EXPIRED("Истёк срок годности"),
    TEMPERATURE_ABUSE("Нарушение температурного режима"),
    CONTAMINATION("Загрязнение"),
    OTHER("Другое (см. описание)");
    
    private final String description;
}
```

## Appendix B: Discrepancy Type Reference

**Existing:**
- BARCODE_MISMATCH
- OVER_QTY
- SSCC_MISMATCH

**New:**
- UNDER_QTY - quantity received is less than expected
- DAMAGE - damaged goods detected
- EXPIRED_PRODUCT - expiry date is in the past
- LOT_MISMATCH - lot number doesn't match expected

## Appendix C: Sample Analytics Dashboard Layout

```
┌─────────────────────────────────────────────────────────┐
│                  Аналитика приёмки                      │
│  Период: [2026-01-01] до [2026-01-31]  [Экспорт CSV]   │
├─────────────────────────────────────────────────────────┤
│  📊 Производительность                                  │
│    ├─ Среднее время приёмки: 2.5 часа                  │
│    ├─ Накладных обработано: 180                        │
│    └─ По статусам: [Bar Chart]                         │
├─────────────────────────────────────────────────────────┤
│  ⚠️  Расхождения                                        │
│    ├─ Процент с расхождениями: 12.5%                   │
│    ├─ UNDER_QTY: 15 | DAMAGE: 8 | EXPIRED: 1           │
│    └─ По типам: [Pie Chart]                            │
├─────────────────────────────────────────────────────────┤
│  📦 Паллеты                                             │
│    ├─ Всего обработано: 465                            │
│    ├─ Повреждённых: 12 (2.7%)                          │
│    └─ По статусам: [Bar Chart]                         │
└─────────────────────────────────────────────────────────┘
```

---

**END OF PLAN**

**Approval Required**: Please review this plan and approve before implementation begins.

**Questions**: Contact supervisor or project manager for clarifications.
