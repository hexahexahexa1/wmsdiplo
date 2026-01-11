package com.wmsdipl.core.domain;

/**
 * Enum for audit log actions.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    STATUS_CHANGE,
    LOCATION_CHANGE,
    WORKFLOW_TRANSITION
}
