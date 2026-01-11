package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.AuditAction;
import com.wmsdipl.core.domain.AuditLog;
import com.wmsdipl.core.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing audit logs.
 * Tracks all important changes to entities in the system.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Logs an entity creation.
     *
     * @param entityType the type of entity (e.g., "Receipt", "Pallet")
     * @param entityId the ID of the created entity
     * @param changedBy the user who created it
     */
    @Transactional
    public void logCreate(String entityType, Long entityId, String changedBy) {
        logChange(entityType, entityId, AuditAction.CREATE, changedBy, null, null, null);
    }

    /**
     * Logs an entity update.
     *
     * @param entityType the type of entity
     * @param entityId the ID of the entity
     * @param changedBy the user who made the change
     * @param fieldName the name of the changed field
     * @param oldValue the old value
     * @param newValue the new value
     */
    @Transactional
    public void logUpdate(String entityType, Long entityId, String changedBy, 
                         String fieldName, String oldValue, String newValue) {
        logChange(entityType, entityId, AuditAction.UPDATE, changedBy, fieldName, oldValue, newValue);
    }

    /**
     * Logs an entity deletion.
     *
     * @param entityType the type of entity
     * @param entityId the ID of the deleted entity
     * @param changedBy the user who deleted it
     */
    @Transactional
    public void logDelete(String entityType, Long entityId, String changedBy) {
        logChange(entityType, entityId, AuditAction.DELETE, changedBy, null, null, null);
    }

    /**
     * Logs a status change.
     *
     * @param entityType the type of entity
     * @param entityId the ID of the entity
     * @param changedBy the user who changed the status
     * @param oldStatus the old status
     * @param newStatus the new status
     */
    @Transactional
    public void logStatusChange(String entityType, Long entityId, String changedBy, 
                               String oldStatus, String newStatus) {
        logChange(entityType, entityId, AuditAction.STATUS_CHANGE, changedBy, 
                 "status", oldStatus, newStatus);
    }

    /**
     * Logs a location change.
     *
     * @param entityType the type of entity (typically "Pallet")
     * @param entityId the ID of the entity
     * @param changedBy the user who moved it
     * @param oldLocation the old location
     * @param newLocation the new location
     */
    @Transactional
    public void logLocationChange(String entityType, Long entityId, String changedBy, 
                                 String oldLocation, String newLocation) {
        logChange(entityType, entityId, AuditAction.LOCATION_CHANGE, changedBy, 
                 "location", oldLocation, newLocation);
    }

    /**
     * Logs a workflow transition.
     *
     * @param entityType the type of entity
     * @param entityId the ID of the entity
     * @param changedBy the user who triggered the transition
     * @param fromState the previous workflow state
     * @param toState the new workflow state
     */
    @Transactional
    public void logWorkflowTransition(String entityType, Long entityId, String changedBy, 
                                     String fromState, String toState) {
        logChange(entityType, entityId, AuditAction.WORKFLOW_TRANSITION, changedBy, 
                 "workflowState", fromState, toState);
    }

    /**
     * Generic method to log any change.
     *
     * @param entityType the type of entity
     * @param entityId the ID of the entity
     * @param action the action performed
     * @param changedBy the user who performed the action
     * @param fieldName the changed field (can be null)
     * @param oldValue the old value (can be null)
     * @param newValue the new value (can be null)
     */
    @Transactional
    public void logChange(String entityType, Long entityId, AuditAction action, String changedBy,
                         String fieldName, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setChangedBy(changedBy);
        log.setFieldName(fieldName);
        log.setOldValue(truncate(oldValue, 512));
        log.setNewValue(truncate(newValue, 512));
        
        auditLogRepository.save(log);
    }

    /**
     * Retrieves audit history for a specific entity.
     *
     * @param entityType the type of entity
     * @param entityId the ID of the entity
     * @return list of audit logs ordered by timestamp descending
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    /**
     * Retrieves audit logs for all entities of a given type.
     *
     * @param entityType the type of entity
     * @return list of audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityTypeHistory(String entityType) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType);
    }

    /**
     * Retrieves audit logs for a specific user.
     *
     * @param username the username
     * @return list of audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getUserActivity(String username) {
        return auditLogRepository.findByChangedByOrderByTimestampDesc(username);
    }

    /**
     * Retrieves audit logs within a time range.
     *
     * @param start start time (inclusive)
     * @param end end time (inclusive)
     * @return list of audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    /**
     * Truncates a string to a maximum length.
     *
     * @param value the string to truncate
     * @param maxLength the maximum length
     * @return truncated string or null if input is null
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
