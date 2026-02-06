package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.ScanDto;
import com.wmsdipl.core.domain.Scan;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Scan entity and ScanDto.
 */
@Component
public class ScanMapper {

    /**
     * Converts Scan entity to ScanDto.
     */
    public ScanDto toDto(Scan scan) {
        if (scan == null) {
            return null;
        }

        return new ScanDto(
            scan.getId(),
            scan.getTask() != null ? scan.getTask().getId() : null,
            scan.getRequestId(),
            scan.getPalletCode(),
            scan.getSscc(),
            scan.getBarcode(),
            scan.getQty(),
            scan.getDeviceId(),
            scan.getDiscrepancy(),
            scan.getDamageFlag(),
            scan.getDamageType(),
            scan.getDamageDescription(),
            scan.getLotNumber(),
            scan.getExpiryDate(),
            scan.getScannedAt(),
            scan.getDuplicate(),
            scan.getIdempotentReplay(),
            scan.getWarnings()
        );
    }
}
