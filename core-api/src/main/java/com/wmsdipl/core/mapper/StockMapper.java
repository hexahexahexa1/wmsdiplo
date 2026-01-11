package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.StockItemDto;
import com.wmsdipl.contracts.dto.StockMovementDto;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import com.wmsdipl.core.domain.Sku;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper for Stock DTOs.
 * Converts between domain entities and stock-related DTOs.
 */
@Component
public class StockMapper {

    /**
     * Convert Pallet entity to StockItemDto.
     * SKU information must be provided separately since Pallet only stores skuId.
     * 
     * @param pallet Pallet entity
     * @param sku SKU entity (can be null)
     * @return StockItemDto
     */
    public StockItemDto toStockItemDto(Pallet pallet, Sku sku) {
        return new StockItemDto(
            pallet.getId(),
            pallet.getCode(),
            pallet.getStatus() != null ? pallet.getStatus().name() : null,
            
            // SKU information
            sku != null ? sku.getId() : pallet.getSkuId(),
            sku != null ? sku.getCode() : null,
            sku != null ? sku.getName() : null,
            
            // Quantity and UOM
            pallet.getQuantity(),
            pallet.getUom(),
            
            // Location information (nullable for unplaced pallets)
            pallet.getLocation() != null ? pallet.getLocation().getId() : null,
            pallet.getLocation() != null ? pallet.getLocation().getCode() : null,
            
            // Receipt information (convert LocalDate to LocalDateTime)
            pallet.getReceipt() != null ? pallet.getReceipt().getId() : null,
            pallet.getReceipt() != null ? pallet.getReceipt().getDocNo() : null,
            pallet.getReceipt() != null && pallet.getReceipt().getDocDate() != null 
                ? pallet.getReceipt().getDocDate().atStartOfDay() 
                : null,
            
            // Product tracking
            pallet.getLotNumber(),
            pallet.getExpiryDate(),
            
            // Timestamps
            pallet.getCreatedAt(),
            pallet.getUpdatedAt()
        );
    }

    /**
     * Convert PalletMovement entity to StockMovementDto.
     * 
     * @param movement PalletMovement entity
     * @return StockMovementDto
     */
    public StockMovementDto toStockMovementDto(PalletMovement movement) {
        return new StockMovementDto(
            movement.getId(),
            movement.getPallet() != null ? movement.getPallet().getId() : null,
            movement.getPallet() != null ? movement.getPallet().getCode() : null,
            movement.getMovementType() != null ? movement.getMovementType().name() : null,
            
            // Location transition
            movement.getFromLocation() != null ? movement.getFromLocation().getId() : null,
            movement.getFromLocation() != null ? movement.getFromLocation().getCode() : null,
            movement.getToLocation() != null ? movement.getToLocation().getId() : null,
            movement.getToLocation() != null ? movement.getToLocation().getCode() : null,
            
            // Quantity
            movement.getQuantity(),
            
            // Related task
            movement.getTaskId(),
            
            // Movement metadata
            movement.getMovedBy(),
            movement.getMovedAt()
        );
    }
}
