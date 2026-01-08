package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.PalletDto;
import com.wmsdipl.core.domain.Pallet;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Pallet entity and PalletDto.
 */
@Component
public class PalletMapper {

    /**
     * Converts Pallet entity to PalletDto.
     * Extracts IDs from related entities to avoid lazy-loading issues.
     */
    public PalletDto toDto(Pallet pallet) {
        if (pallet == null) {
            return null;
        }

        return new PalletDto(
            pallet.getId(),
            pallet.getCode(),
            pallet.getCodeType(),
            pallet.getStatus() != null ? pallet.getStatus().name() : null,
            pallet.getSkuId(),
            pallet.getUom(),
            pallet.getQuantity(),
            pallet.getLocation() != null ? pallet.getLocation().getId() : null,
            pallet.getLocation() != null ? pallet.getLocation().getCode() : null,
            pallet.getReceipt() != null ? pallet.getReceipt().getId() : null,
            pallet.getReceiptLine() != null ? pallet.getReceiptLine().getId() : null,
            pallet.getLotNumber(),
            pallet.getExpiryDate(),
            pallet.getWeightKg(),
            pallet.getHeightCm(),
            pallet.getCreatedAt(),
            pallet.getUpdatedAt()
        );
    }
}
