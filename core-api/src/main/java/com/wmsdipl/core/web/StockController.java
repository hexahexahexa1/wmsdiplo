package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.StockItemDto;
import com.wmsdipl.contracts.dto.StockMovementDto;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletMovement;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.mapper.StockMapper;
import com.wmsdipl.core.service.StockService;
import com.wmsdipl.core.service.StockService.StockResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for stock inventory queries.
 * Provides current and historical views of warehouse inventory.
 */
@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock", description = "Stock inventory and movement history operations")
public class StockController {

    private final StockService stockService;
    private final StockMapper stockMapper;

    public StockController(StockService stockService, StockMapper stockMapper) {
        this.stockService = stockService;
        this.stockMapper = stockMapper;
    }

    /**
     * Get paginated stock inventory with optional filters.
     * 
     * @param skuCode SKU code filter (optional)
     * @param locationCode Location code filter (optional)
     * @param palletBarcode Pallet barcode filter (optional)
     * @param receiptId Receipt ID filter (optional)
     * @param status Pallet status filter (optional)
     * @param asOfDate Point-in-time date (optional, ISO format: 2026-01-11T10:30:00)
     * @param pageable Pagination parameters (default: page=0, size=50, sort by id)
     * @return Page of stock items
     */
    @GetMapping
    @Operation(
        summary = "Get stock inventory", 
        description = "Retrieves paginated warehouse inventory with optional filters. " +
                     "Supports point-in-time queries via asOfDate parameter. " +
                     "Default page size: 50, max: 500."
    )
    public Page<StockItemDto> getStock(
            @Parameter(description = "Filter by SKU code")
            @RequestParam(required = false) String skuCode,
            
            @Parameter(description = "Filter by location code")
            @RequestParam(required = false) String locationCode,
            
            @Parameter(description = "Filter by pallet barcode (partial match)")
            @RequestParam(required = false) String palletBarcode,
            
            @Parameter(description = "Filter by receipt ID")
            @RequestParam(required = false) Long receiptId,
            
            @Parameter(description = "Filter by pallet status (RECEIVED, PLACED, etc.)")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Point-in-time date (ISO format: 2026-01-11T10:30:00)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime asOfDate,
            
            @PageableDefault(size = 50, sort = "id") Pageable pageable
    ) {
        StockResult result = stockService.getStock(
                skuCode, locationCode, palletBarcode, receiptId, status, asOfDate, pageable);
        
        Map<Long, Sku> skuMap = result.skuMap;
        return result.pallets.map(pallet -> 
            stockMapper.toStockItemDto(pallet, skuMap.get(pallet.getSkuId()))
        );
    }

    /**
     * Get single pallet details by ID.
     * 
     * @param palletId Pallet ID
     * @return Stock item details
     */
    @GetMapping("/pallet/{palletId}")
    @Operation(
        summary = "Get pallet details", 
        description = "Retrieves detailed information for a single pallet"
    )
    public StockItemDto getPallet(
            @Parameter(description = "Pallet ID")
            @PathVariable Long palletId
    ) {
        StockResult result = stockService.getPalletById(palletId);
        Pallet pallet = result.singlePallet;
        Sku sku = result.skuMap.get(pallet.getSkuId());
        return stockMapper.toStockItemDto(pallet, sku);
    }

    /**
     * Get movement history for a specific pallet.
     * 
     * @param palletId Pallet ID
     * @return List of movements ordered by date descending
     */
    @GetMapping("/pallet/{palletId}/history")
    @Operation(
        summary = "Get pallet movement history", 
        description = "Retrieves chronological movement history for a pallet (newest first)"
    )
    public List<StockMovementDto> getPalletHistory(
            @Parameter(description = "Pallet ID")
            @PathVariable Long palletId
    ) {
        List<PalletMovement> movements = stockService.getPalletHistory(palletId);
        return movements.stream()
                .map(stockMapper::toStockMovementDto)
                .collect(Collectors.toList());
    }

    /**
     * Get pallets in a specific location.
     * 
     * @param locationId Location ID
     * @param pageable Pagination parameters
     * @return Page of pallets in the location
     */
    @GetMapping("/location/{locationId}")
    @Operation(
        summary = "Get pallets by location", 
        description = "Retrieves all pallets currently stored in a specific location"
    )
    public Page<StockItemDto> getPalletsByLocation(
            @Parameter(description = "Location ID")
            @PathVariable Long locationId,
            
            @PageableDefault(size = 50, sort = "id") Pageable pageable
    ) {
        StockResult result = stockService.getPalletsByLocation(locationId, pageable);
        Map<Long, Sku> skuMap = result.skuMap;
        return result.pallets.map(pallet -> 
            stockMapper.toStockItemDto(pallet, skuMap.get(pallet.getSkuId()))
        );
    }
}
