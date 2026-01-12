package com.wmsdipl.contracts.dto;

import java.util.List;

/**
 * Generic result for bulk operations.
 * Contains lists of successful items and failures with error messages.
 *
 * @param <T> Type of successful items (e.g., Long for IDs, String for codes)
 */
public record BulkOperationResult<T>(
    List<T> successes,
    List<BulkOperationFailure> failures
) {
}
