package com.wmsdipl.contracts.dto;

import java.util.List;

/**
 * Result of bulk pallet creation operation.
 */
public record PalletCreationResult(
    List<String> created,              // List of created pallet codes (PLT-XXX)
    List<BulkOperationFailure> failures // List of failures
) {
}
