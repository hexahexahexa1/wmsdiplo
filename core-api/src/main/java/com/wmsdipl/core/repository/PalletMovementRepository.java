package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PalletMovementRepository extends JpaRepository<PalletMovement, Long> {
    List<PalletMovement> findByPallet(Pallet pallet);
    
    @Query("SELECT pm FROM PalletMovement pm " +
           "LEFT JOIN FETCH pm.pallet " +
           "LEFT JOIN FETCH pm.fromLocation " +
           "LEFT JOIN FETCH pm.toLocation " +
           "WHERE pm.pallet = :pallet " +
           "ORDER BY pm.movedAt DESC")
    List<PalletMovement> findByPalletOrderByMovedAtDesc(@Param("pallet") Pallet pallet);
    
    @Query("SELECT pm FROM PalletMovement pm WHERE pm.pallet = :pallet AND pm.movedAt <= :asOfDate ORDER BY pm.movedAt DESC")
    List<PalletMovement> findByPalletBeforeDate(@Param("pallet") Pallet pallet, @Param("asOfDate") LocalDateTime asOfDate);
    
    Optional<PalletMovement> findTopByPalletAndMovedAtLessThanEqualOrderByMovedAtDesc(Pallet pallet, LocalDateTime asOfDate);
}
