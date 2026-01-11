package com.wmsdipl.core.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for validating barcodes and scanning operations.
 * Supports SSCC, EAN-13, EAN-8, and internal barcode formats.
 */
@Service
public class BarcodeValidationService {

    // SSCC: 18 digits (Serial Shipping Container Code)
    private static final Pattern SSCC_PATTERN = Pattern.compile("^\\d{18}$");
    
    // EAN-13: 13 digits
    private static final Pattern EAN13_PATTERN = Pattern.compile("^\\d{13}$");
    
    // EAN-8: 8 digits
    private static final Pattern EAN8_PATTERN = Pattern.compile("^\\d{8}$");
    
    // Internal codes: alphanumeric with optional dashes/underscores
    private static final Pattern INTERNAL_PATTERN = Pattern.compile("^[A-Z0-9_-]{4,32}$");

    /**
     * Validates a barcode based on its type.
     *
     * @param barcode the barcode to validate
     * @param type the expected barcode type (SSCC, EAN13, EAN8, INTERNAL)
     * @return validation result
     */
    public ValidationResult validate(String barcode, String type) {
        if (barcode == null || barcode.isBlank()) {
            return ValidationResult.invalid("Barcode cannot be empty");
        }

        String normalizedBarcode = barcode.trim().toUpperCase();
        
        return switch (type != null ? type.toUpperCase() : "AUTO") {
            case "SSCC" -> validateSSCC(normalizedBarcode);
            case "EAN13" -> validateEAN13(normalizedBarcode);
            case "EAN8" -> validateEAN8(normalizedBarcode);
            case "INTERNAL" -> validateInternal(normalizedBarcode);
            case "AUTO" -> autoDetectAndValidate(normalizedBarcode);
            default -> ValidationResult.invalid("Unknown barcode type: " + type);
        };
    }

    /**
     * Auto-detects barcode type and validates.
     *
     * @param barcode the barcode to validate
     * @return validation result
     */
    public ValidationResult autoDetectAndValidate(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return ValidationResult.invalid("Barcode cannot be empty");
        }

        String normalized = barcode.trim();

        // Try SSCC first (18 digits)
        if (SSCC_PATTERN.matcher(normalized).matches()) {
            return validateSSCC(normalized);
        }

        // Try EAN-13
        if (EAN13_PATTERN.matcher(normalized).matches()) {
            return validateEAN13(normalized);
        }

        // Try EAN-8
        if (EAN8_PATTERN.matcher(normalized).matches()) {
            return validateEAN8(normalized);
        }

        // Try internal format
        if (INTERNAL_PATTERN.matcher(normalized.toUpperCase()).matches()) {
            return ValidationResult.valid("INTERNAL", normalized.toUpperCase());
        }

        return ValidationResult.invalid("Barcode format not recognized: " + barcode);
    }

    /**
     * Validates SSCC barcode with check digit verification.
     *
     * @param barcode 18-digit SSCC
     * @return validation result
     */
    private ValidationResult validateSSCC(String barcode) {
        if (!SSCC_PATTERN.matcher(barcode).matches()) {
            return ValidationResult.invalid("SSCC must be 18 digits");
        }

        if (!verifyGS1CheckDigit(barcode)) {
            return ValidationResult.invalid("SSCC check digit is invalid");
        }

        return ValidationResult.valid("SSCC", barcode);
    }

    /**
     * Validates EAN-13 barcode with check digit verification.
     *
     * @param barcode 13-digit EAN
     * @return validation result
     */
    private ValidationResult validateEAN13(String barcode) {
        if (!EAN13_PATTERN.matcher(barcode).matches()) {
            return ValidationResult.invalid("EAN-13 must be 13 digits");
        }

        if (!verifyEANCheckDigit(barcode)) {
            return ValidationResult.invalid("EAN-13 check digit is invalid");
        }

        return ValidationResult.valid("EAN13", barcode);
    }

    /**
     * Validates EAN-8 barcode with check digit verification.
     *
     * @param barcode 8-digit EAN
     * @return validation result
     */
    private ValidationResult validateEAN8(String barcode) {
        if (!EAN8_PATTERN.matcher(barcode).matches()) {
            return ValidationResult.invalid("EAN-8 must be 8 digits");
        }

        if (!verifyEANCheckDigit(barcode)) {
            return ValidationResult.invalid("EAN-8 check digit is invalid");
        }

        return ValidationResult.valid("EAN8", barcode);
    }

    /**
     * Validates internal barcode format.
     *
     * @param barcode internal code
     * @return validation result
     */
    private ValidationResult validateInternal(String barcode) {
        String normalized = barcode.toUpperCase();
        
        if (!INTERNAL_PATTERN.matcher(normalized).matches()) {
            return ValidationResult.invalid("Internal barcode must be 4-32 alphanumeric characters");
        }

        return ValidationResult.valid("INTERNAL", normalized);
    }

    /**
     * Verifies GS1 check digit (used for SSCC).
     * Algorithm: multiply digits alternately by 3 and 1, sum, and check modulo 10.
     *
     * @param barcode the barcode with check digit as last character
     * @return true if check digit is valid
     */
    private boolean verifyGS1CheckDigit(String barcode) {
        if (barcode.length() < 2) {
            return false;
        }

        String digits = barcode.substring(0, barcode.length() - 1);
        char checkDigitChar = barcode.charAt(barcode.length() - 1);
        int expectedCheckDigit = Character.getNumericValue(checkDigitChar);

        int sum = 0;
        int multiplier = 3; // Start with 3 for rightmost digit before check digit

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(digits.charAt(i));
            sum += digit * multiplier;
            multiplier = (multiplier == 3) ? 1 : 3;
        }

        int calculatedCheckDigit = (10 - (sum % 10)) % 10;
        return calculatedCheckDigit == expectedCheckDigit;
    }

    /**
     * Verifies EAN check digit (used for EAN-8 and EAN-13).
     * Algorithm: similar to GS1 but alternates 1 and 3 from left to right.
     *
     * @param barcode the barcode with check digit as last character
     * @return true if check digit is valid
     */
    private boolean verifyEANCheckDigit(String barcode) {
        if (barcode.length() < 2) {
            return false;
        }

        String digits = barcode.substring(0, barcode.length() - 1);
        char checkDigitChar = barcode.charAt(barcode.length() - 1);
        int expectedCheckDigit = Character.getNumericValue(checkDigitChar);

        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int digit = Character.getNumericValue(digits.charAt(i));
            int multiplier = (i % 2 == 0) ? 1 : 3;
            sum += digit * multiplier;
        }

        int calculatedCheckDigit = (10 - (sum % 10)) % 10;
        return calculatedCheckDigit == expectedCheckDigit;
    }

    /**
     * Validation result record.
     */
    public record ValidationResult(
            boolean isValid,
            String type,
            String normalizedBarcode,
            String errorMessage
    ) {
        public static ValidationResult valid(String type, String normalizedBarcode) {
            return new ValidationResult(true, type, normalizedBarcode, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, null, null, errorMessage);
        }
    }
}
