package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    java.util.Optional<Scan> findFirstByTaskOrderByScannedAtDescIdDesc(Task task);

    void deleteByTask(Task task);

    Optional<Scan> findByTaskIdAndRequestId(Long taskId, String requestId);

    Optional<Scan> findFirstByTaskIdAndPalletCodeOrderByScannedAtDesc(Long taskId, String palletCode);

    boolean existsByTaskIdAndScannedAtAfter(Long taskId, LocalDateTime scannedAt);

    boolean existsByTask_Line_IdIn(java.util.Collection<Long> lineIds);

    boolean existsByBarcodeIgnoreCase(String barcode);
    boolean existsByTask_Receipt_IdAndBarcodeIgnoreCase(Long receiptId, String barcode);
}
