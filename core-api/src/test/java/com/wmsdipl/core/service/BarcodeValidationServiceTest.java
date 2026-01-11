package com.wmsdipl.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BarcodeValidationService.
 * Tests barcode format validation and check digit verification.
 */
class BarcodeValidationServiceTest {

    private BarcodeValidationService service;

    @BeforeEach
    void setUp() {
        service = new BarcodeValidationService();
    }

    @Test
    void shouldValidateSSCC_WhenValidFormat() {
        // Given - valid SSCC with correct check digit
        String validSSCC = "106141411234567897"; // Extension digit + GS1 company prefix + serial + check digit

        // When
        BarcodeValidationService.ValidationResult result = service.validate(validSSCC, "SSCC");

        // Then
        assertTrue(result.isValid());
        assertEquals("SSCC", result.type());
        assertEquals(validSSCC, result.normalizedBarcode());
    }

    @Test
    void shouldRejectSSCC_WhenInvalidCheckDigit() {
        // Given - SSCC with wrong check digit
        String invalidSSCC = "106141411234567890";

        // When
        BarcodeValidationService.ValidationResult result = service.validate(invalidSSCC, "SSCC");

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("check digit"));
    }

    @Test
    void shouldRejectSSCC_WhenWrongLength() {
        // Given
        String shortSSCC = "1061414112345";

        // When
        BarcodeValidationService.ValidationResult result = service.validate(shortSSCC, "SSCC");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("18 digits"));
    }

    @Test
    void shouldValidateEAN13_WhenValidFormat() {
        // Given - valid EAN-13 with correct check digit
        String validEAN13 = "4006381333931"; // Real EAN-13

        // When
        BarcodeValidationService.ValidationResult result = service.validate(validEAN13, "EAN13");

        // Then
        assertTrue(result.isValid());
        assertEquals("EAN13", result.type());
    }

    @Test
    void shouldRejectEAN13_WhenInvalidCheckDigit() {
        // Given
        String invalidEAN13 = "4006381333930"; // Wrong check digit

        // When
        BarcodeValidationService.ValidationResult result = service.validate(invalidEAN13, "EAN13");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("check digit"));
    }

    @Test
    void shouldValidateEAN8_WhenValidFormat() {
        // Given - valid EAN-8
        String validEAN8 = "96385074"; // Real EAN-8

        // When
        BarcodeValidationService.ValidationResult result = service.validate(validEAN8, "EAN8");

        // Then
        assertTrue(result.isValid());
        assertEquals("EAN8", result.type());
    }

    @Test
    void shouldValidateInternal_WhenValidFormat() {
        // Given
        String validInternal = "PLT-12345";

        // When
        BarcodeValidationService.ValidationResult result = service.validate(validInternal, "INTERNAL");

        // Then
        assertTrue(result.isValid());
        assertEquals("INTERNAL", result.type());
        assertEquals("PLT-12345", result.normalizedBarcode());
    }

    @Test
    void shouldNormalizeInternal_ToUpperCase() {
        // Given
        String lowercase = "plt-12345";

        // When
        BarcodeValidationService.ValidationResult result = service.validate(lowercase, "INTERNAL");

        // Then
        assertTrue(result.isValid());
        assertEquals("PLT-12345", result.normalizedBarcode());
    }

    @Test
    void shouldRejectInternal_WhenTooShort() {
        // Given
        String tooShort = "ABC";

        // When
        BarcodeValidationService.ValidationResult result = service.validate(tooShort, "INTERNAL");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("4-32"));
    }

    @Test
    void shouldRejectInternal_WhenTooLong() {
        // Given
        String tooLong = "A".repeat(33);

        // When
        BarcodeValidationService.ValidationResult result = service.validate(tooLong, "INTERNAL");

        // Then
        assertFalse(result.isValid());
    }

    @Test
    void shouldAutoDetect_SSCC() {
        // Given
        String sscc = "106141411234567897";

        // When
        BarcodeValidationService.ValidationResult result = service.autoDetectAndValidate(sscc);

        // Then
        assertTrue(result.isValid());
        assertEquals("SSCC", result.type());
    }

    @Test
    void shouldAutoDetect_EAN13() {
        // Given
        String ean13 = "4006381333931";

        // When
        BarcodeValidationService.ValidationResult result = service.autoDetectAndValidate(ean13);

        // Then
        assertTrue(result.isValid());
        assertEquals("EAN13", result.type());
    }

    @Test
    void shouldAutoDetect_EAN8() {
        // Given
        String ean8 = "96385074";

        // When
        BarcodeValidationService.ValidationResult result = service.autoDetectAndValidate(ean8);

        // Then
        assertTrue(result.isValid());
        assertEquals("EAN8", result.type());
    }

    @Test
    void shouldAutoDetect_Internal() {
        // Given
        String internal = "PLT-12345";

        // When
        BarcodeValidationService.ValidationResult result = service.autoDetectAndValidate(internal);

        // Then
        assertTrue(result.isValid());
        assertEquals("INTERNAL", result.type());
    }

    @Test
    void shouldReject_EmptyBarcode() {
        // When
        BarcodeValidationService.ValidationResult result = service.validate("", "SSCC");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("empty"));
    }

    @Test
    void shouldReject_NullBarcode() {
        // When
        BarcodeValidationService.ValidationResult result = service.validate(null, "SSCC");

        // Then
        assertFalse(result.isValid());
    }

    @Test
    void shouldReject_UnknownType() {
        // When
        BarcodeValidationService.ValidationResult result = service.validate("123456", "UNKNOWN");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Unknown"));
    }

    @Test
    void shouldReject_UnrecognizedFormat() {
        // Given - doesn't match any known pattern
        String unrecognized = "invalid@barcode!";

        // When
        BarcodeValidationService.ValidationResult result = service.autoDetectAndValidate(unrecognized);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("not recognized"));
    }

    @Test
    void shouldTrimWhitespace_WhenValidating() {
        // Given
        String withSpaces = "  PLT-12345  ";

        // When
        BarcodeValidationService.ValidationResult result = service.validate(withSpaces, "INTERNAL");

        // Then
        assertTrue(result.isValid());
        assertEquals("PLT-12345", result.normalizedBarcode());
    }
}
