package com.wmsdipl.core.service;

import com.wmsdipl.core.repository.PalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DuplicateScanDetectionService.
 * Tests duplicate detection and scan caching logic.
 */
@ExtendWith(MockitoExtension.class)
class DuplicateScanDetectionServiceTest {

    @Mock
    private PalletRepository palletRepository;

    private DuplicateScanDetectionService service;

    @BeforeEach
    void setUp() {
        service = new DuplicateScanDetectionService(palletRepository);
        service.clearAllScans(); // Clear cache before each test
    }

    @Test
    void shouldAllowFirstScan_WhenBarcodeNotScannedBefore() {
        // Given
        String barcode = "PLT-001";
        when(palletRepository.existsByCode(barcode)).thenReturn(false);

        // When
        DuplicateScanDetectionService.ScanResult result = service.checkScan(barcode);

        // Then
        assertTrue(result.isValid());
        assertFalse(result.isDuplicate());
        assertFalse(result.existsInDatabase());
        assertEquals(barcode, result.barcode());
    }

    @Test
    void shouldDetectDuplicate_WhenScannedTwiceQuickly() throws InterruptedException {
        // Given
        String barcode = "PLT-001";
        when(palletRepository.existsByCode(barcode)).thenReturn(false);

        // When - scan first time
        DuplicateScanDetectionService.ScanResult firstScan = service.checkScan(barcode);
        
        // Wait a bit but less than window
        Thread.sleep(1000);
        
        // Scan again
        DuplicateScanDetectionService.ScanResult secondScan = service.checkScan(barcode);

        // Then
        assertTrue(firstScan.isValid());
        assertFalse(firstScan.isDuplicate());
        
        assertFalse(secondScan.isValid());
        assertTrue(secondScan.isDuplicate());
        assertNotNull(secondScan.lastScanTime());
    }

    @Test
    void shouldAllowRescan_AfterClearingScan() {
        // Given
        String barcode = "PLT-001";
        when(palletRepository.existsByCode(barcode)).thenReturn(false);

        // When
        service.checkScan(barcode); // First scan
        service.clearScan(barcode);  // Clear from cache
        DuplicateScanDetectionService.ScanResult result = service.checkScan(barcode); // Scan again

        // Then
        assertTrue(result.isValid());
        assertFalse(result.isDuplicate());
    }

    @Test
    void shouldIndicateExistence_WhenBarcodeInDatabase() {
        // Given
        String barcode = "PLT-EXISTING";
        when(palletRepository.existsByCode(barcode)).thenReturn(true);

        // When
        DuplicateScanDetectionService.ScanResult result = service.checkScan(barcode);

        // Then
        assertTrue(result.isValid());
        assertFalse(result.isDuplicate());
        assertTrue(result.existsInDatabase());
    }

    @Test
    void shouldNormalizeBarcode_ToUpperCase() {
        // Given
        String lowercase = "plt-001";
        when(palletRepository.existsByCode("PLT-001")).thenReturn(false);

        // When
        DuplicateScanDetectionService.ScanResult result = service.checkScan(lowercase);

        // Then
        assertTrue(result.isValid());
        assertEquals("PLT-001", result.barcode());
    }

    @Test
    void shouldReject_EmptyBarcode() {
        // When
        DuplicateScanDetectionService.ScanResult result = service.checkScan("");

        // Then
        assertFalse(result.isValid());
        assertFalse(result.isDuplicate());
    }

    @Test
    void shouldReject_NullBarcode() {
        // When
        DuplicateScanDetectionService.ScanResult result = service.checkScan(null);

        // Then
        assertFalse(result.isValid());
    }

    @Test
    void shouldClearAllScans_WhenRequested() {
        // Given
        when(palletRepository.existsByCode("PLT-001")).thenReturn(false);
        when(palletRepository.existsByCode("PLT-002")).thenReturn(false);
        
        service.checkScan("PLT-001");
        service.checkScan("PLT-002");

        // When
        service.clearAllScans();
        DuplicateScanDetectionService.ScanResult result1 = service.checkScan("PLT-001");
        DuplicateScanDetectionService.ScanResult result2 = service.checkScan("PLT-002");

        // Then
        assertTrue(result1.isValid());
        assertFalse(result1.isDuplicate());
        assertTrue(result2.isValid());
        assertFalse(result2.isDuplicate());
    }

    @Test
    void shouldHandleMultipleDifferentBarcodes() {
        // Given
        String barcode1 = "PLT-001";
        String barcode2 = "PLT-002";
        when(palletRepository.existsByCode(barcode1)).thenReturn(false);
        when(palletRepository.existsByCode(barcode2)).thenReturn(false);

        // When
        DuplicateScanDetectionService.ScanResult result1 = service.checkScan(barcode1);
        DuplicateScanDetectionService.ScanResult result2 = service.checkScan(barcode2);

        // Then
        assertTrue(result1.isValid());
        assertFalse(result1.isDuplicate());
        assertTrue(result2.isValid());
        assertFalse(result2.isDuplicate());
    }

    @Test
    void shouldTrimWhitespace_BeforeProcessing() {
        // Given
        String withSpaces = "  PLT-001  ";
        when(palletRepository.existsByCode("PLT-001")).thenReturn(false);

        // When
        DuplicateScanDetectionService.ScanResult result = service.checkScan(withSpaces);

        // Then
        assertTrue(result.isValid());
        assertEquals("PLT-001", result.barcode());
    }
}
