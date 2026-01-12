package com.wmsdipl.contracts.dto;

/**
 * Represents a single failure in a bulk operation.
 *
 * @param id Item ID that failed
 * @param error Error message
 */
public record BulkOperationFailure(
    Long id,
    String error
) {
}
