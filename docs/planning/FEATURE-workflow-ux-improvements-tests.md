# Feature Implementation & Test Coverage Guide: Workflow & UX Improvements

## 1. Logic Change: Prevent Early Completion of Placement Tasks

### Description
Placement tasks (`TaskType.PLACEMENT`) must strictly follow the physical scanning workflow (`recordPlacement`). The manual "Complete" button/API endpoint is now blocked for these tasks to prevent process bypass.

### Test Scenarios
*   **Unit Test (`TaskLifecycleServiceTest`):**
    *   `shouldThrowException_WhenCompletingPlacementTaskManually`: Create a task with type `PLACEMENT`. Call `complete(taskId)`. Assert `ResponseStatusException` (Bad Request) is thrown.
    *   `shouldCompleteReceivingTaskManually`: Create a task with type `RECEIVING`. Call `complete(taskId)`. Assert task status becomes `COMPLETED`.

## 2. Notification: Task Creation Count

### Description
When "Start Receiving" or "Start Placement" is triggered, the API now returns the number of tasks created. The UI displays this in a popup notification.

### Test Scenarios
*   **Integration Test (`ReceiptControllerTest`):**
    *   `startReceiving_ShouldReturnTaskCount`: Call `POST /api/receipts/{id}/start-receiving`. Assert response body contains `{"count": N}` where N > 0.
    *   `startPlacement_ShouldReturnTaskCount`: Call `POST /api/receipts/{id}/start-placement`. Assert response body contains `{"count": N}`.
*   **UI Test (Manual):**
    *   Click "Start Receiving". Verify popup "Создано N задач" appears in bottom-right.

## 3. Workflow Automation: Auto-transition Receipt Status

### Description
Receipt status automatically transitions from `IN_PROGRESS` to `ACCEPTED` (or `READY_FOR_SHIPMENT` for cross-dock) immediately when the last associated `RECEIVING` task is completed.

### Test Scenarios
*   **Integration Test (`ReceivingWorkflowServiceTest`):**
    *   `shouldAutoCompleteReceipt_WhenLastTaskCompleted`:
        1.  Create a receipt with 2 lines.
        2.  Start receiving (creates 2 tasks).
        3.  Complete task 1. Receipt status should remain `IN_PROGRESS`.
        4.  Complete task 2. Receipt status should change to `ACCEPTED`.
    *   `shouldTransitionToReadyForShipment_WhenCrossDock`: Same as above, but with `crossDock = true`. Final status should be `READY_FOR_SHIPMENT`.

## 4. UI Fix: Terminal Scrolling

### Description
The scanning screen ("Fact" tab) is now wrapped in a `ScrollPane` to ensure accessibility on small screens.

### Test Scenarios
*   **UI Test (Manual):**
    *   Open a task in Terminal mode.
    *   Resize window to be very small height.
    *   Verify vertical scrollbar appears and "Submit" buttons can be reached.

## 5. Localization (English/Russian)

### Description
Application supports switching between English and Russian via Settings.

### Test Scenarios
*   **UI Test (Manual):**
    *   Go to Settings. Change language to English.
    *   Restart application.
    *   Verify main menu labels are in English ("Receipts" instead of "Приходы").
    *   Change back to Russian. Restart. Verify Russian labels.
