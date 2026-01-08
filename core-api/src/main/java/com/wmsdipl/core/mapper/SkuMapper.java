package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.CreateSkuRequest;
import com.wmsdipl.contracts.dto.SkuDto;
import com.wmsdipl.core.domain.Sku;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Sku entities and DTOs.
 */
@Component
public class SkuMapper {

    public SkuDto toDto(Sku sku) {
        if (sku == null) {
            return null;
        }
        return new SkuDto(
            sku.getId(),
            sku.getCode(),
            sku.getName(),
            sku.getUom()
        );
    }

    public Sku toEntity(CreateSkuRequest request) {
        if (request == null) {
            return null;
        }
        Sku sku = new Sku();
        sku.setCode(request.code());
        sku.setName(request.name());
        sku.setUom(request.uom());
        return sku;
    }

    public void updateEntity(Sku sku, CreateSkuRequest request) {
        if (sku == null || request == null) {
            return;
        }
        sku.setCode(request.code());
        sku.setName(request.name());
        sku.setUom(request.uom());
    }
}
