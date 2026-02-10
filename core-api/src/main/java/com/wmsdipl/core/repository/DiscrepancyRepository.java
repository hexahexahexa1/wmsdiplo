package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, Long>, JpaSpecificationExecutor<Discrepancy> {
    List<Discrepancy> findByReceipt(Receipt receipt);
    List<Discrepancy> findByScanId(Long scanId);
    long deleteByScanId(Long scanId);
    List<Discrepancy> findByDraftSkuId(Long draftSkuId);
    List<Discrepancy> findByReceipt_IdAndDraftSkuId(Long receiptId, Long draftSkuId);
    List<Discrepancy> findByResolvedFalse();
    List<Discrepancy> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
    long countByResolvedTrueAndCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByTypeInAndCreatedAtBetween(Collection<String> types, LocalDateTime from, LocalDateTime to);
    boolean existsByLine_SkuIdAndResolvedFalseAndReceipt_StatusIn(Long skuId, Collection<ReceiptStatus> statuses);
    boolean existsByDraftSkuIdAndResolvedFalseAndReceipt_StatusIn(Long draftSkuId, Collection<ReceiptStatus> statuses);
}
