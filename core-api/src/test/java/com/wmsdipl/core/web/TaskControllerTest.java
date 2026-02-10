package com.wmsdipl.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.contracts.dto.ScanDto;
import com.wmsdipl.contracts.dto.TaskDto;
import com.wmsdipl.contracts.dto.UndoLastScanResultDto;
import com.wmsdipl.core.domain.*;
import com.wmsdipl.core.mapper.ScanMapper;
import com.wmsdipl.core.mapper.TaskMapper;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.service.TaskService;
import com.wmsdipl.core.service.workflow.PlacementWorkflowService;
import com.wmsdipl.core.service.workflow.ReceivingWorkflowService;
import com.wmsdipl.core.service.workflow.ShippingWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API tests for TaskController using MockMvc.
 * Tests the HTTP layer, request validation, and response formatting.
 */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private ReceivingWorkflowService receivingWorkflowService;

    @MockBean
    private PlacementWorkflowService placementWorkflowService;

    @MockBean
    private ShippingWorkflowService shippingWorkflowService;

    @MockBean
    private ScanRepository scanRepository;

    @MockBean
    private ScanMapper scanMapper;

    @MockBean
    private TaskMapper taskMapper;

    @Test
    void shouldListAllTasks_WhenCalled() throws Exception {
        // Given
        Task task = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.findAll()).thenReturn(List.of(task));
        when(taskMapper.toDtoList(List.of(task))).thenReturn(List.of(dto));

        // When & Then
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].taskType").value("RECEIVING"));
    }

    @Test
    void shouldListTasksByReceipt_WhenReceiptIdProvided() throws Exception {
        // Given
        Task task = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.findByReceipt(100L)).thenReturn(List.of(task));
        when(taskMapper.toDtoList(List.of(task))).thenReturn(List.of(dto));

        // When & Then
        mockMvc.perform(get("/api/tasks").param("receiptId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(taskService).findByReceipt(100L);
    }

    @Test
    void shouldListTasksWithPagination_WhenPageRequested() throws Exception {
        Task task = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.findFiltered(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(task), PageRequest.of(0, 10), 1));
        when(taskMapper.toDtoList(List.of(task))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/tasks").param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1));

        verify(taskService).findFiltered(isNull(), isNull(), isNull(), isNull(), isNull(), any());
        verify(taskMapper).toDtoList(List.of(task));
        verify(taskMapper, never()).toDto(any(Task.class));
    }

    @Test
    void shouldFilterTasksByTaskId_WhenTaskIdProvided() throws Exception {
        Task task = createMockTask(99L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(99L);

        when(taskService.findFiltered(isNull(), isNull(), isNull(), isNull(), eq(99L), any()))
            .thenReturn(new PageImpl<>(List.of(task), PageRequest.of(0, 10), 1));
        when(taskMapper.toDtoList(List.of(task))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/tasks").param("page", "0").param("size", "10").param("taskId", "99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(99));

        verify(taskService).findFiltered(isNull(), isNull(), isNull(), isNull(), eq(99L), any());
    }

    @Test
    void shouldGetTaskById_WhenValidId() throws Exception {
        // Given
        Task task = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.get(1L)).thenReturn(task);
        when(taskMapper.toDto(task)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taskType").value("RECEIVING"));
    }

    @Test
    @WithMockUser(username = "supervisor1", roles = {"SUPERVISOR"})
    void shouldAssignTask_WhenValidRequest() throws Exception {
        // Given
        Task assigned = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.assign(1L, "operator1", "supervisor1")).thenReturn(assigned);
        when(taskMapper.toDto(assigned)).thenReturn(dto);

        String requestBody = """
                {
                    "assignee": "operator1",
                    "assignedBy": "supervisor1"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/tasks/1/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(taskService).assign(1L, "operator1", "supervisor1");
    }

    @Test
    @WithMockUser(username = "operator1", roles = {"OPERATOR"})
    void shouldRejectAssignToAnotherUser_WhenOperatorAssignsTask() throws Exception {
        String requestBody = """
                {
                    "assignee": "operator2",
                    "assignedBy": "operator1"
                }
                """;

        mockMvc.perform(post("/api/tasks/1/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verify(taskService, never()).get(anyLong());
        verify(taskService, never()).assign(anyLong(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "operator1", roles = {"OPERATOR"})
    void shouldRejectReassign_WhenOperatorAssignsNonNewTask() throws Exception {
        // Given
        Task assignedTask = createMockTask(1L, TaskType.RECEIVING);
        when(assignedTask.getStatus()).thenReturn(TaskStatus.ASSIGNED);
        when(taskService.get(1L)).thenReturn(assignedTask);

        String requestBody = """
                {
                    "assignee": "operator1",
                    "assignedBy": "operator1"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/tasks/1/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(taskService).get(1L);
        verify(taskService, never()).assign(anyLong(), anyString(), anyString());
    }

    @Test
    void shouldStartTask_WhenValidId() throws Exception {
        // Given
        Task started = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.start(1L)).thenReturn(started);
        when(taskMapper.toDto(started)).thenReturn(dto);

        // When & Then
        mockMvc.perform(post("/api/tasks/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(taskService).start(1L);
    }

    @Test
    void shouldCompleteTask_WhenValidId() throws Exception {
        // Given
        Task completed = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.complete(1L)).thenReturn(completed);
        when(taskMapper.toDto(completed)).thenReturn(dto);

        // When & Then
        mockMvc.perform(post("/api/tasks/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(taskService).complete(1L);
    }

    @Test
    void shouldCancelTask_WhenValidId() throws Exception {
        // Given
        Task cancelled = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.cancel(1L)).thenReturn(cancelled);
        when(taskMapper.toDto(cancelled)).thenReturn(dto);

        // When & Then
        mockMvc.perform(post("/api/tasks/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(taskService).cancel(1L);
    }

    @Test
    void shouldReleaseTask_WhenValidId() throws Exception {
        // Given
        Task released = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.release(1L)).thenReturn(released);
        when(taskMapper.toDto(released)).thenReturn(dto);

        // When & Then
        mockMvc.perform(post("/api/tasks/1/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(taskService).release(1L);
    }

    @Test
    void shouldUndoLastScan_WhenValidId() throws Exception {
        UndoLastScanResultDto result = new UndoLastScanResultDto(
            1L,
            10L,
            "RECEIVING",
            BigDecimal.TEN,
            BigDecimal.ONE,
            "PALLET-001",
            true,
            1
        );

        when(taskService.undoLastScan(1L)).thenReturn(result);

        mockMvc.perform(post("/api/tasks/1/undo-last-scan"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(1))
            .andExpect(jsonPath("$.scanId").value(10))
            .andExpect(jsonPath("$.movementRolledBack").value(true));

        verify(taskService).undoLastScan(1L);
    }

    @Test
    void shouldGetTaskScans_WhenValidId() throws Exception {
        // Given
        Task task = createMockTask(1L, TaskType.RECEIVING);
        Scan scan = createMockScan(1L);
        ScanDto scanDto = new ScanDto(
                1L,                 // id
                1L,                 // taskId
                null,               // requestId
                "PALLET-001",       // palletCode
                "SSCC-001",         // sscc
                "BARCODE-001",      // barcode
                BigDecimal.TEN,     // qty
                "DEVICE-01",        // deviceId
                false,              // discrepancy
                false,              // damageFlag
                null,               // damageType
                null,               // damageDescription
                null,               // lotNumber
                null,               // expiryDate
                LocalDateTime.now(), // scannedAt
                false,              // duplicate
                false,              // idempotentReplay
                List.of()           // warnings
        );

        when(taskService.get(1L)).thenReturn(task);
        when(scanRepository.findByTaskOrderByScannedAtDesc(task)).thenReturn(List.of(scan));
        when(scanMapper.toDto(scan)).thenReturn(scanDto);

        // When & Then
        mockMvc.perform(get("/api/tasks/1/scans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].barcode").value("BARCODE-001"));
    }

    @Test
    void shouldRecordReceivingScan_WhenTaskTypeIsReceiving() throws Exception {
        // Given
        Task task = createMockTask(1L, TaskType.RECEIVING);
        Scan scan = createMockScan(1L);
        ScanDto scanDto = new ScanDto(
                1L,                 // id
                1L,                 // taskId
                null,               // requestId
                "PALLET-001",       // palletCode
                "SSCC-001",         // sscc
                "BARCODE-001",      // barcode
                BigDecimal.TEN,     // qty
                "DEVICE-01",        // deviceId
                false,              // discrepancy
                false,              // damageFlag
                null,               // damageType
                null,               // damageDescription
                null,               // lotNumber
                null,               // expiryDate
                LocalDateTime.now(), // scannedAt
                false,              // duplicate
                false,              // idempotentReplay
                List.of()           // warnings
        );

        when(taskService.get(1L)).thenReturn(task);
        when(receivingWorkflowService.recordScan(eq(1L), any(RecordScanRequest.class))).thenReturn(scan);
        when(scanMapper.toDto(scan)).thenReturn(scanDto);

        String requestBody = """
                {
                    "palletCode": "PALLET-001",
                    "qty": 10,
                    "sscc": "SSCC-001",
                    "barcode": "BARCODE-001"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/tasks/1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value("BARCODE-001"))
                .andExpect(jsonPath("$.qty").value(10));

        verify(receivingWorkflowService).recordScan(eq(1L), any(RecordScanRequest.class));
    }

    @Test
    void shouldRecordPlacementScan_WhenTaskTypeIsPlacement() throws Exception {
        // Given
        Task task = createMockTask(1L, TaskType.PLACEMENT);
        Scan scan = createMockScan(1L);
        ScanDto scanDto = new ScanDto(
                1L,                 // id
                1L,                 // taskId
                null,               // requestId
                "PALLET-001",       // palletCode
                "SSCC-001",         // sscc
                "BARCODE-001",      // barcode
                BigDecimal.TEN,     // qty
                "DEVICE-01",        // deviceId
                false,              // discrepancy
                false,              // damageFlag
                null,               // damageType
                null,               // damageDescription
                null,               // lotNumber
                null,               // expiryDate
                LocalDateTime.now(), // scannedAt
                false,              // duplicate
                false,              // idempotentReplay
                List.of()           // warnings
        );

        when(taskService.get(1L)).thenReturn(task);
        when(placementWorkflowService.recordPlacement(eq(1L), any(RecordScanRequest.class))).thenReturn(scan);
        when(scanMapper.toDto(scan)).thenReturn(scanDto);

        String requestBody = """
                {
                    "palletCode": "PALLET-001",
                    "qty": 10,
                    "sscc": "SSCC-001",
                    "barcode": "BARCODE-001"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/tasks/1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value("BARCODE-001"));

        verify(placementWorkflowService).recordPlacement(eq(1L), any(RecordScanRequest.class));
    }

    @Test
    void shouldRecordShippingScan_WhenTaskTypeIsShipping() throws Exception {
        Task task = createMockTask(1L, TaskType.SHIPPING);
        Scan scan = createMockScan(1L);
        ScanDto scanDto = new ScanDto(
                1L,
                1L,
                null,
                "PALLET-001",
                "SSCC-001",
                "BARCODE-001",
                BigDecimal.TEN,
                "DEVICE-01",
                false,
                false,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                false,
                false,
                List.of()
        );

        when(taskService.get(1L)).thenReturn(task);
        when(shippingWorkflowService.recordShipping(eq(1L), any(RecordScanRequest.class))).thenReturn(scan);
        when(scanMapper.toDto(scan)).thenReturn(scanDto);

        String requestBody = """
                {
                    "palletCode": "PALLET-001",
                    "qty": 10,
                    "sscc": "SSCC-001",
                    "barcode": "BARCODE-001"
                }
                """;

        mockMvc.perform(post("/api/tasks/1/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value("BARCODE-001"));

        verify(shippingWorkflowService).recordShipping(eq(1L), any(RecordScanRequest.class));
    }

    @Test
    void shouldGenerateTasks_WhenValidRequest() throws Exception {
        // Given
        doNothing().when(taskService).createReceivingTasks(100L, TaskType.RECEIVING, 5);

        String requestBody = """
                {
                    "receiptId": 100,
                    "count": 5,
                    "taskType": "RECEIVING"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/tasks/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted());

        verify(taskService).createReceivingTasks(100L, TaskType.RECEIVING, 5);
    }

    @Test
    void shouldReturnBadRequest_WhenGenerateRequestInvalid() throws Exception {
        // Given - invalid count (zero)
        String requestBody = """
                {
                    "receiptId": 100,
                    "count": 0
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/tasks/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createReceivingTasks(anyLong(), any(), anyInt());
    }

    @Test
    void shouldCheckDiscrepancies_WhenCalled() throws Exception {
        // Given
        when(taskService.hasDiscrepancies(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/tasks/1/has-discrepancies"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(taskService).hasDiscrepancies(1L);
    }

    // Helper methods

    private Task createMockTask(Long id, TaskType taskType) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(id);
        when(task.getTaskType()).thenReturn(taskType);
        when(task.getStatus()).thenReturn(TaskStatus.NEW);
        return task;
    }

    private TaskDto createMockTaskDto(Long id) {
        return new TaskDto(
                id,                     // id
                "RECEIVING",            // taskType (String)
                "NEW",                  // status (String)
                null,                   // assignee
                null,                   // assignedBy
                null,                   // palletId
                null,                   // sourceLocationId
                null,                   // targetLocationId
                null,                   // targetLocationCode
                null,                   // receiptId
                null,                   // receiptDocNo
                null,                   // lineId
                null,                   // skuId
                BigDecimal.ZERO,        // qtyAssigned
                BigDecimal.ZERO,        // qtyDone
                null,                   // lineUom
                null,                   // baseUom
                null,                   // unitFactorToBase
                null,                   // skuCode
                null,                   // palletCode
                null,                   // priority
                LocalDateTime.now(),    // createdAt
                null,                   // startedAt
                null                    // closedAt
        );
    }

    private Scan createMockScan(Long id) {
        Scan scan = mock(Scan.class);
        when(scan.getId()).thenReturn(id);
        return scan;
    }
}
