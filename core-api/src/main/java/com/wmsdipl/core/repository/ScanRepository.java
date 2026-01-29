package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScanRepository extends JpaRepository<Scan, Long> {
    List<Scan> findByTask(Task task);
    
    /**
     * Finds all scans for a task, ordered by scan time (newest first).
     * Used in terminal to display scan history.
     * 
     * @param task the task
     * @return scans ordered by scannedAt descending
     */
    List<Scan> findByTaskOrderByScannedAtDesc(Task task);

    void deleteByTask(Task task);
}
