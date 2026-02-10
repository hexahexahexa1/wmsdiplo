package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReceiptLineRepository extends JpaRepository<ReceiptLine, Long> {
    boolean existsBySkuIdAndUomIgnoreCaseAndReceipt_StatusIn(
        Long skuId,
        String uom,
        Collection<ReceiptStatus> statuses
    );

    List<ReceiptLine> findBySkuId(Long skuId);
    List<ReceiptLine> findByReceipt_IdAndSkuId(Long receiptId, Long skuId);

    @Query("""
        select (count(rl) > 0)
        from ReceiptLine rl
        where rl.skuId = :skuId
          and rl.receipt.status in :statuses
          and (rl.excludedFromWorkflow is null or rl.excludedFromWorkflow = false)
    """)
    boolean existsActiveBySkuIdAndReceiptStatusIn(
        @Param("skuId") Long skuId,
        @Param("statuses") Collection<ReceiptStatus> statuses
    );
}
