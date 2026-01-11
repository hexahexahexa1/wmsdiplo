package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.AuditAction;
import com.wmsdipl.core.domain.AuditLog;
import com.wmsdipl.core.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogService.
 * Tests audit logging functionality and history retrieval.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        // Setup common mocks if needed
    }

    @Test
    void shouldLogCreate_WhenEntityCreated() {
        // Given
        String entityType = "Receipt";
        Long entityId = 1L;
        String changedBy = "testuser";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditLogService.logCreate(entityType, entityId, changedBy);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertEquals(entityType, captured.getEntityType());
        assertEquals(entityId, captured.getEntityId());
        assertEquals(AuditAction.CREATE, captured.getAction());
        assertEquals(changedBy, captured.getChangedBy());
        assertNull(captured.getFieldName());
    }

    @Test
    void shouldLogUpdate_WhenFieldChanged() {
        // Given
        String entityType = "Pallet";
        Long entityId = 2L;
        String changedBy = "admin";
        String fieldName = "quantity";
        String oldValue = "10";
        String newValue = "15";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditLogService.logUpdate(entityType, entityId, changedBy, fieldName, oldValue, newValue);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertEquals(entityType, captured.getEntityType());
        assertEquals(entityId, captured.getEntityId());
        assertEquals(AuditAction.UPDATE, captured.getAction());
        assertEquals(changedBy, captured.getChangedBy());
        assertEquals(fieldName, captured.getFieldName());
        assertEquals(oldValue, captured.getOldValue());
        assertEquals(newValue, captured.getNewValue());
    }

    @Test
    void shouldLogDelete_WhenEntityDeleted() {
        // Given
        String entityType = "Task";
        Long entityId = 3L;
        String changedBy = "supervisor";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditLogService.logDelete(entityType, entityId, changedBy);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertEquals(AuditAction.DELETE, captured.getAction());
        assertEquals(entityType, captured.getEntityType());
        assertEquals(entityId, captured.getEntityId());
    }

    @Test
    void shouldLogStatusChange_WhenStatusUpdated() {
        // Given
        String entityType = "Receipt";
        Long entityId = 1L;
        String changedBy = "operator";
        String oldStatus = "DRAFT";
        String newStatus = "CONFIRMED";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditLogService.logStatusChange(entityType, entityId, changedBy, oldStatus, newStatus);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertEquals(AuditAction.STATUS_CHANGE, captured.getAction());
        assertEquals("status", captured.getFieldName());
        assertEquals(oldStatus, captured.getOldValue());
        assertEquals(newStatus, captured.getNewValue());
    }

    @Test
    void shouldLogLocationChange_WhenPalletMoved() {
        // Given
        String entityType = "Pallet";
        Long entityId = 5L;
        String changedBy = "operator";
        String oldLocation = "A-01-01";
        String newLocation = "B-02-03";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditLogService.logLocationChange(entityType, entityId, changedBy, oldLocation, newLocation);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertEquals(AuditAction.LOCATION_CHANGE, captured.getAction());
        assertEquals("location", captured.getFieldName());
        assertEquals(oldLocation, captured.getOldValue());
        assertEquals(newLocation, captured.getNewValue());
    }

    @Test
    void shouldLogWorkflowTransition_WhenWorkflowChanges() {
        // Given
        String entityType = "Receipt";
        Long entityId = 1L;
        String changedBy = "supervisor";
        String fromState = "RECEIVING";
        String toState = "ACCEPTED";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditLogService.logWorkflowTransition(entityType, entityId, changedBy, fromState, toState);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertEquals(AuditAction.WORKFLOW_TRANSITION, captured.getAction());
        assertEquals("workflowState", captured.getFieldName());
        assertEquals(fromState, captured.getOldValue());
        assertEquals(toState, captured.getNewValue());
    }

    @Test
    void shouldRetrieveEntityHistory_WhenRequested() {
        // Given
        String entityType = "Pallet";
        Long entityId = 10L;
        List<AuditLog> expectedLogs = List.of(new AuditLog(), new AuditLog());

        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId))
                .thenReturn(expectedLogs);

        // When
        List<AuditLog> result = auditLogService.getEntityHistory(entityType, entityId);

        // Then
        assertEquals(expectedLogs.size(), result.size());
        verify(auditLogRepository).findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    @Test
    void shouldRetrieveEntityTypeHistory_WhenRequested() {
        // Given
        String entityType = "Receipt";
        List<AuditLog> expectedLogs = List.of(new AuditLog(), new AuditLog(), new AuditLog());

        when(auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType))
                .thenReturn(expectedLogs);

        // When
        List<AuditLog> result = auditLogService.getEntityTypeHistory(entityType);

        // Then
        assertEquals(expectedLogs.size(), result.size());
        verify(auditLogRepository).findByEntityTypeOrderByTimestampDesc(entityType);
    }

    @Test
    void shouldRetrieveUserActivity_WhenRequested() {
        // Given
        String username = "testuser";
        List<AuditLog> expectedLogs = List.of(new AuditLog(), new AuditLog());

        when(auditLogRepository.findByChangedByOrderByTimestampDesc(username))
                .thenReturn(expectedLogs);

        // When
        List<AuditLog> result = auditLogService.getUserActivity(username);

        // Then
        assertEquals(expectedLogs.size(), result.size());
        verify(auditLogRepository).findByChangedByOrderByTimestampDesc(username);
    }

    @Test
    void shouldRetrieveLogsByTimeRange_WhenRequested() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<AuditLog> expectedLogs = List.of(new AuditLog());

        when(auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end))
                .thenReturn(expectedLogs);

        // When
        List<AuditLog> result = auditLogService.getLogsByTimeRange(start, end);

        // Then
        assertEquals(expectedLogs.size(), result.size());
        verify(auditLogRepository).findByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    @Test
    void shouldTruncateLongValues_WhenSaving() {
        // Given
        String entityType = "Receipt";
        Long entityId = 1L;
        String changedBy = "testuser";
        String fieldName = "notes";
        String longValue = "A".repeat(600); // Exceeds 512 char limit
        String newValue = "B";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditLogService.logUpdate(entityType, entityId, changedBy, fieldName, longValue, newValue);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertNotNull(captured.getOldValue());
        assertEquals(512, captured.getOldValue().length());
    }

    @Test
    void shouldHandleNullValues_WhenLogging() {
        // Given
        String entityType = "Pallet";
        Long entityId = 1L;
        String changedBy = "testuser";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditLogService.logUpdate(entityType, entityId, changedBy, null, null, null);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertNull(captured.getFieldName());
        assertNull(captured.getOldValue());
        assertNull(captured.getNewValue());
    }
}
