package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.CreateZoneRequest;
import com.wmsdipl.contracts.dto.UpdateZoneRequest;
import com.wmsdipl.contracts.dto.ZoneDto;
import com.wmsdipl.core.domain.Zone;
import org.springframework.stereotype.Component;

/**
 * Mapper для преобразования между Zone entity и DTOs.
 */
@Component
public class ZoneMapper {
    
    /**
     * Преобразует Zone entity в ZoneDto.
     *
     * @param zone entity для преобразования
     * @return DTO с данными зоны
     */
    public ZoneDto toDto(Zone zone) {
        if (zone == null) {
            return null;
        }
        
        return new ZoneDto(
            zone.getId(),
            zone.getCode(),
            zone.getName(),
            zone.getPriorityRank(),
            zone.getDescription(),
            zone.getActive()
        );
    }
    
    /**
     * Создает новую Zone entity из CreateZoneRequest.
     *
     * @param request запрос на создание
     * @return новая Zone entity
     */
    public Zone toEntity(CreateZoneRequest request) {
        if (request == null) {
            return null;
        }
        
        Zone zone = new Zone();
        zone.setCode(request.code());
        zone.setName(request.name());
        zone.setPriorityRank(request.priorityRank() != null ? request.priorityRank() : 100);
        zone.setDescription(request.description());
        
        return zone;
    }
    
    /**
     * Обновляет существующую Zone entity данными из UpdateZoneRequest.
     *
     * @param zone существующая entity для обновления
     * @param request запрос с новыми данными
     */
    public void updateEntity(Zone zone, UpdateZoneRequest request) {
        if (zone == null || request == null) {
            return;
        }
        
        if (request.code() != null) {
            zone.setCode(request.code());
        }
        
        if (request.name() != null) {
            zone.setName(request.name());
        }
        
        if (request.priorityRank() != null) {
            zone.setPriorityRank(request.priorityRank());
        }
        
        if (request.description() != null) {
            zone.setDescription(request.description());
        }
        
        if (request.active() != null) {
            zone.setActive(request.active());
        }
    }
}
