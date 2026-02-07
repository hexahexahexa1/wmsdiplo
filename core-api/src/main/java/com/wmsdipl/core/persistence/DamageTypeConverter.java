package com.wmsdipl.core.persistence;

import com.wmsdipl.contracts.dto.DamageType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class DamageTypeConverter implements AttributeConverter<DamageType, String> {

    @Override
    public String convertToDatabaseColumn(DamageType attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public DamageType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        String value = dbData.trim();
        if (value.isEmpty()) {
            return null;
        }

        return switch (value.toUpperCase(Locale.ROOT)) {
            case "PHYSICAL", "PHYSICAL_DAMAGE" -> DamageType.PHYSICAL_DAMAGE;
            case "WATER", "WATER_DAMAGE" -> DamageType.WATER_DAMAGE;
            case "EXPIRED", "EXPIRED_PRODUCT" -> DamageType.EXPIRED;
            case "TEMPERATURE", "TEMP_ABUSE", "TEMPERATURE_ABUSE" -> DamageType.TEMPERATURE_ABUSE;
            case "CONTAMINATION" -> DamageType.CONTAMINATION;
            case "OTHER" -> DamageType.OTHER;
            default -> DamageType.OTHER;
        };
    }
}
