package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface PalletRepository extends JpaRepository<Pallet, Long>, JpaSpecificationExecutor<Pallet> {
    Optional<Pallet> findByCode(String code);
    boolean existsByCode(String code);
    List<Pallet> findByReceiptAndStatus(Receipt receipt, PalletStatus status);
    List<Pallet> findByReceipt(Receipt receipt);
    List<Pallet> findByCreatedAtBetweenAndReceiptIsNotNull(LocalDateTime from, LocalDateTime to);
    List<Pallet> findByLocation(Location location);
    List<Pallet> findByReceipt_IdAndSkuId(Long receiptId, Long skuId);
    List<Pallet> findByReceipt_IdAndReceiptLine_IdIn(Long receiptId, Collection<Long> lineIds);
    long countByLocation(Location location);
    boolean existsByReceiptLine_IdIn(Collection<Long> lineIds);
    boolean existsByReceipt_IdAndSkuId(Long receiptId, Long skuId);
    boolean existsBySkuIdAndQuantityGreaterThan(Long skuId, BigDecimal quantity);
}
