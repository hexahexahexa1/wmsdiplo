package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    Optional<Receipt> findByMessageId(String messageId);
    boolean existsByDocNoAndSupplier(String docNo, String supplier);
}
