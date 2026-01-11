package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.CreateLocationRequest;
import com.wmsdipl.contracts.dto.LocationDto;
import com.wmsdipl.contracts.dto.UpdateLocationRequest;
import com.wmsdipl.core.domain.Location;
import com.wmsdipl.core.domain.LocationStatus;
import com.wmsdipl.core.domain.LocationType;
import com.wmsdipl.core.domain.Zone;
import org.springframework.stereotype.Component;

/**
 * Mapper для преобразования между Location entity и DTOs.
 */
@Component
public class LocationMapper {
    
    /**
     * Преобразует Location entity в LocationDto.
     *
     * @param location entity для преобразования
     * @return DTO с данными локации
     */
    public LocationDto toDto(Location location) {
        if (location == null) {
            return null;
        }
        
        return new LocationDto(
            location.getId(),
            location.getZone() != null ? location.getZone().getId() : null,
            location.getZone() != null ? location.getZone().getCode() : null,
            location.getCode(),
            location.getAisle(),
            location.getBay(),
            location.getLevel(),
            location.getXCoord(),
            location.getYCoord(),
            location.getZCoord(),
            location.getMaxWeightKg(),
            location.getMaxHeightCm(),
            location.getMaxWidthCm(),
            location.getMaxDepthCm(),
            location.getMaxPallets(),
            location.getLocationType() != null ? location.getLocationType().name() : null,
            location.getStatus() != null ? location.getStatus().name() : null,
            location.getActive()
        );
    }
    
    /**
     * Создает новую Location entity из CreateLocationRequest.
     *
     * @param request запрос на создание
     * @param zone зона, к которой принадлежит ячейка
     * @return новая Location entity
     */
    public Location toEntity(CreateLocationRequest request, Zone zone) {
        if (request == null) {
            return null;
        }
        
        Location location = new Location();
        location.setZone(zone);
        location.setCode(request.code());
        location.setAisle(request.aisle());
        location.setBay(request.bay());
        location.setLevel(request.level());
        location.setXCoord(request.xCoord());
        location.setYCoord(request.yCoord());
        location.setZCoord(request.zCoord());
        location.setMaxWeightKg(request.maxWeightKg());
        location.setMaxHeightCm(request.maxHeightCm());
        location.setMaxWidthCm(request.maxWidthCm());
        location.setMaxDepthCm(request.maxDepthCm());
        location.setMaxPallets(request.maxPallets());
        
        if (request.locationType() != null) {
            location.setLocationType(LocationType.valueOf(request.locationType()));
        }
        
        return location;
    }
    
    /**
     * Обновляет существующую Location entity данными из UpdateLocationRequest.
     *
     * @param location существующая entity для обновления
     * @param request запрос с новыми данными
     * @param zone новая зона (если изменилась)
     */
    public void updateEntity(Location location, UpdateLocationRequest request, Zone zone) {
        if (location == null || request == null) {
            return;
        }
        
        if (zone != null) {
            location.setZone(zone);
        }
        
        if (request.code() != null) {
            location.setCode(request.code());
        }
        
        if (request.aisle() != null) {
            location.setAisle(request.aisle());
        }
        
        if (request.bay() != null) {
            location.setBay(request.bay());
        }
        
        if (request.level() != null) {
            location.setLevel(request.level());
        }
        
        if (request.xCoord() != null) {
            location.setXCoord(request.xCoord());
        }
        
        if (request.yCoord() != null) {
            location.setYCoord(request.yCoord());
        }
        
        if (request.zCoord() != null) {
            location.setZCoord(request.zCoord());
        }
        
        if (request.maxWeightKg() != null) {
            location.setMaxWeightKg(request.maxWeightKg());
        }
        
        if (request.maxHeightCm() != null) {
            location.setMaxHeightCm(request.maxHeightCm());
        }
        
        if (request.maxWidthCm() != null) {
            location.setMaxWidthCm(request.maxWidthCm());
        }
        
        if (request.maxDepthCm() != null) {
            location.setMaxDepthCm(request.maxDepthCm());
        }
        
        if (request.maxPallets() != null) {
            location.setMaxPallets(request.maxPallets());
        }
        
        if (request.locationType() != null) {
            location.setLocationType(LocationType.valueOf(request.locationType()));
        }
        
        if (request.status() != null) {
            location.setStatus(LocationStatus.valueOf(request.status()));
        }
        
        if (request.active() != null) {
            location.setActive(request.active());
        }
    }
}
