package com.wmsdipl.core.persistence;

import com.wmsdipl.contracts.dto.DamageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DamageTypeConverterTest {

    private final DamageTypeConverter converter = new DamageTypeConverter();

    @Test
    void shouldReturnCanonicalEnum_WhenLegacyValueProvided() {
        assertEquals(DamageType.PHYSICAL_DAMAGE, converter.convertToEntityAttribute("PHYSICAL"));
        assertEquals(DamageType.WATER_DAMAGE, converter.convertToEntityAttribute("water"));
        assertEquals(DamageType.TEMPERATURE_ABUSE, converter.convertToEntityAttribute("TEMP_ABUSE"));
    }

    @Test
    void shouldReturnOther_WhenUnknownValueProvided() {
        assertEquals(DamageType.OTHER, converter.convertToEntityAttribute("SOMETHING_NEW"));
    }

    @Test
    void shouldReturnNull_WhenDbValueBlankOrNull() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(" "));
    }

    @Test
    void shouldReturnCanonicalDbValue_WhenEnumProvided() {
        assertEquals("PHYSICAL_DAMAGE", converter.convertToDatabaseColumn(DamageType.PHYSICAL_DAMAGE));
        assertNull(converter.convertToDatabaseColumn(null));
    }
}
