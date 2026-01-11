package com.wmsdipl.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmsdipl.contracts.dto.RecordScanRequest;
import com.wmsdipl.contracts.dto.ScanDto;
import com.wmsdipl.contracts.dto.TaskDto;
import com.wmsdipl.core.domain.*;
import com.wmsdipl.core.mapper.DiscrepancyMapper;
import com.wmsdipl.core.mapper.ScanMapper;
import com.wmsdipl.core.mapper.TaskMapper;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.service.TaskService;
import com.wmsdipl.core.service.workflow.PlacementWorkflowService;
import com.wmsdipl.core.service.workflow.ReceivingWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
    private ScanRepository scanRepository;

    @MockBean
    private ScanMapper scanMapper;

    @MockBean
    private TaskMapper taskMapper;

    @MockBean
    private DiscrepancyMapper discrepancyMapper;

    @Test
    void shouldListAllTasks_WhenCalled() throws Exception {
        // Given
        Task task = createMockTask(1L, TaskType.RECEIVING);
        TaskDto dto = createMockTaskDto(1L);

        when(taskService.findAll()).thenReturn(List.of(task));
        when(taskMapper.toDto(task)).thenReturn(dto);

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
        when(taskMapper.toDto(task)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/tasks").param("receiptId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(taskService).findByReceipt(100L);
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
    void shouldGetTaskScans_WhenValidId() throws Exception {
        // Given
        Task task = createMockTask(1L, TaskType.RECEIVING);
        Scan scan = createMockScan(1L);
        ScanDto scanDto = new ScanDto(
                1L,                 // id
                1L,                 // taskId
                "PALLET-001",       // palletCode
                "SSCC-001",         // sscc
                "BARCODE-001",      // barcode
                BigDecimal.TEN,     // qty
                "DEVICE-01",        // deviceId
                false,              // discrepancy
                LocalDateTime.now() // scannedAt
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
                "PALLET-001",       // palletCode
                "SSCC-001",         // sscc
                "BARCODE-001",      // barcode
                BigDecimal.TEN,     // qty
                "DEVICE-01",        // deviceId
                false,              // discrepancy
                LocalDateTime.now() // scannedAt
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
                "PALLET-001",       // palletCode
                "SSCC-001",         // sscc
                "BARCODE-001",      // barcode
                BigDecimal.TEN,     // qty
                "DEVICE-01",        // deviceId
                false,              // discrepancy
                LocalDateTime.now() // scannedAt
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
    void shouldListOpenDiscrepancies_WhenCalled() throws Exception {
        // Given
        Discrepancy discrepancy = new Discrepancy();
        when(taskService.findOpenDiscrepancies()).thenReturn(List.of(discrepancy));

        // When & Then
        mockMvc.perform(get("/api/tasks/discrepancies/open"))
                .andExpect(status().isOk());

        verify(taskService).findOpenDiscrepancies();
    }

    @Test
    void shouldResolveDiscrepancy_WhenValidRequest() throws Exception {
        // Given
        Discrepancy resolved = new Discrepancy();
        when(taskService.resolveDiscrepancy(1L, "Fixed")).thenReturn(resolved);

        String requestBody = """
                {
                    "comment": "Fixed"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/tasks/discrepancies/1/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(taskService).resolveDiscrepancy(1L, "Fixed");
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
