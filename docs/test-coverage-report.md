# Test Coverage Report

## Implemented Tests

### 1. Logic Change: Prevent Early Completion of Placement Tasks
**File:** `core-api/src/test/java/com/wmsdipl/core/service/TaskLifecycleServiceTest.java`
*   `shouldThrowException_WhenCompletingPlacementTaskManually`: Verified that `PLACEMENT` tasks throw `ResponseStatusException` when `complete()` is called.
*   `shouldCompleteReceivingTaskManually`: Verified that `RECEIVING` tasks can still be completed manually.

### 2. Notification: Task Creation Count
**File:** `core-api/src/test/java/com/wmsdipl/core/web/ReceiptControllerTest.java`
*   `shouldStartReceiving_AndReturnCount`: Verified that `POST /api/receipts/{id}/start-receiving` returns status 202 and JSON body `{"count": N}`.
*   `shouldStartPlacement_AndReturnCount`: Verified that `POST /api/receipts/{id}/start-placement` returns status 202 and JSON body `{"count": N}`.

**File:** `core-api/src/test/java/com/wmsdipl/core/service/workflow/ReceivingWorkflowServiceTest.java`
*   `shouldStartReceiving_AndReturnCount`: Verified service method returns integer count.

### 3. Workflow Automation: Auto-transition Receipt Status
**File:** `core-api/src/test/java/com/wmsdipl/core/service/workflow/ReceivingWorkflowServiceTest.java`
*   `shouldAutoCompleteReceipt_WhenLastTaskCompleted`: Verified that `checkAndCompleteReceipt` transitions status to `ACCEPTED` when all tasks are COMPLETED.
*   `shouldTransitionToReadyForShipment_WhenCrossDock`: Verified transition to `READY_FOR_SHIPMENT` for cross-dock receipts.

**File:** `core-api/src/test/java/com/wmsdipl/core/service/TaskServiceTest.java`
*   `shouldCompleteTask_WhenValidId`: Verified that `complete()` triggers `receivingWorkflowService.checkAndCompleteReceipt(receiptId)`.

## Results
All new tests passed successfully.
Existing unit tests for `TaskController` and `ReceiptController` were updated to align with API changes.
