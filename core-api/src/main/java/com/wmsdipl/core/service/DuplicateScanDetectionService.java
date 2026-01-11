package com.wmsdipl.core.service;

import com.wmsdipl.core.repository.PalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for detecting duplicate scans and managing scan sessions.
 * Prevents accidental double-scanning of barcodes within a time window.
 */
@Service
public class DuplicateScanDetectionService {

    private final PalletRepository palletRepository;
    
    // In-memory cache for recent scans (barcode -> timestamp)
    private final Map<String, LocalDateTime> recentScans = new ConcurrentHashMap<>();
    
    // Duplicate detection window in seconds
    private static final int DUPLICATE_WINDOW_SECONDS = 5;

    public DuplicateScanDetectionService(PalletRepository palletRepository) {
        this.palletRepository = palletRepository;
    }

    /**
     * Checks if a barcode scan is a duplicate within the time window.
     *
     * @param barcode the scanned barcode
     * @return scan result indicating if it's a duplicate
     */
    @Transactional(readOnly = true)
    public ScanResult checkScan(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return ScanResult.invalid("Barcode cannot be empty");
        }

        String normalizedBarcode = barcode.trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        // Check if recently scanned
        LocalDateTime lastScan = recentScans.get(normalizedBarcode);
        if (lastScan != null) {
            long secondsSinceLastScan = java.time.Duration.between(lastScan, now).getSeconds();
            if (secondsSinceLastScan < DUPLICATE_WINDOW_SECONDS) {
                return ScanResult.duplicate(normalizedBarcode, lastScan);
            }
        }

        // Check if barcode exists in database
        boolean exists = palletRepository.existsByCode(normalizedBarcode);

        // Record this scan
        recentScans.put(normalizedBarcode, now);

        // Clean up old entries (older than window)
        cleanupOldScans(now);

        if (exists) {
            return ScanResult.existsInDatabase(normalizedBarcode);
        }

        return ScanResult.valid(normalizedBarcode);
    }

    /**
     * Clears the scan cache for a specific barcode.
     *
     * @param barcode the barcode to clear from cache
     */
    public void clearScan(String barcode) {
        if (barcode != null) {
            recentScans.remove(barcode.trim().toUpperCase());
        }
    }

    /**
     * Clears all cached scans.
     */
    public void clearAllScans() {
        recentScans.clear();
    }

    /**
     * Removes scan entries older than the duplicate window.
     *
     * @param now current timestamp
     */
    private void cleanupOldScans(LocalDateTime now) {
        recentScans.entrySet().removeIf(entry -> {
            long age = java.time.Duration.between(entry.getValue(), now).getSeconds();
            return age > DUPLICATE_WINDOW_SECONDS * 2; // Keep for 2x window to be safe
        });
    }

    /**
     * Scan result record.
     */
    public record ScanResult(
            boolean isValid,
            boolean isDuplicate,
            boolean existsInDatabase,
            String barcode,
            LocalDateTime lastScanTime,
            String message
    ) {
        public static ScanResult valid(String barcode) {
            return new ScanResult(true, false, false, barcode, null, "Scan valid");
        }

        public static ScanResult duplicate(String barcode, LocalDateTime lastScanTime) {
            return new ScanResult(
                    false, 
                    true, 
                    false, 
                    barcode, 
                    lastScanTime, 
                    "Duplicate scan detected within " + DUPLICATE_WINDOW_SECONDS + " seconds"
            );
        }

        public static ScanResult existsInDatabase(String barcode) {
            return new ScanResult(
                    true, 
                    false, 
                    true, 
                    barcode, 
                    null, 
                    "Barcode already exists in database"
            );
        }

        public static ScanResult invalid(String message) {
            return new ScanResult(false, false, false, null, null, message);
        }
    }
}
