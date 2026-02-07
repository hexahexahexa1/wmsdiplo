package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ReceiptLineRepository extends JpaRepository<ReceiptLine, Long> {
    boolean existsBySkuIdAndUomIgnoreCaseAndReceipt_StatusIn(
        Long skuId,
        String uom,
        Collection<ReceiptStatus> statuses
    );
}
