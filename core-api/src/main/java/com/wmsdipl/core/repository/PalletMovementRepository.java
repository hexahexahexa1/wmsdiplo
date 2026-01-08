package com.wmsdipl.core.repository;

import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PalletMovementRepository extends JpaRepository<PalletMovement, Long> {
    List<PalletMovement> findByPallet(Pallet pallet);
}
