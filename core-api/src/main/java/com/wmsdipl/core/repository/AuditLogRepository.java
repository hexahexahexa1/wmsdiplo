package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);
    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);
    List<AuditLog> findByChangedByOrderByTimestampDesc(String changedBy);
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}
