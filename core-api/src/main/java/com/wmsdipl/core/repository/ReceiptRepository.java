package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {
    Optional<Receipt> findByMessageId(String messageId);
    boolean existsByDocNoAndSupplier(String docNo, String supplier);
    List<Receipt> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<Receipt> findByCreatedAtBetweenAndStatusIn(
        LocalDateTime from,
        LocalDateTime to,
        Collection<ReceiptStatus> statuses
    );
    List<Receipt> findByStatusAndUpdatedAtBefore(ReceiptStatus status, LocalDateTime updatedAtBefore);
    List<Receipt> findByCrossDockTrueAndOutboundRef(String outboundRef);
    List<Receipt> findByCrossDockTrueAndOutboundRefIsNotNull();
}
