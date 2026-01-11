package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating CSV exports from data.
 * Provides utilities for converting data to CSV format with proper escaping.
 */
@Service
public class CsvExportService {

    /**
     * Generates CSV content from headers and rows.
     *
     * @param headers list of column headers
     * @param rows list of data rows (each row is a list of string values)
     * @return CSV content as byte array (UTF-8 with BOM for Excel compatibility)
     */
    public byte[] generateCsv(List<String> headers, List<List<String>> rows) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(osw)) {

            // Write UTF-8 BOM for Excel compatibility
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);

            // Write headers
            writer.println(toCsvLine(headers));

            // Write data rows
            for (List<String> row : rows) {
                writer.println(toCsvLine(row));
            }

            writer.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    /**
     * Converts a list of values to a CSV line with proper escaping.
     *
     * @param values list of values
     * @return CSV line string
     */
    private String toCsvLine(List<String> values) {
        return values.stream()
                .map(this::escapeCsvValue)
                .collect(Collectors.joining(","));
    }

    /**
     * Escapes a single CSV value.
     * Wraps in quotes if contains comma, quote, or newline.
     * Doubles any quotes within the value.
     *
     * @param value the value to escape
     * @return escaped CSV value
     */
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value;

        // If value contains quotes, double them
        if (value.contains("\"")) {
            escaped = value.replace("\"", "\"\"");
        }

        // If value contains comma, quote, or newline, wrap in quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            escaped = "\"" + escaped + "\"";
        }

        return escaped;
    }

    /**
     * Converts any value to string for CSV export.
     * Handles null values gracefully.
     *
     * @param value the value to convert
     * @return string representation
     */
    public String toStringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    /**
     * Exports receipts to CSV format.
     *
     * @param receipts list of receipts to export
     * @return CSV content as byte array
     */
    public byte[] exportReceipts(List<Receipt> receipts) {
        List<String> headers = List.of(
                "ID", "Doc No", "Doc Date", "Supplier", "Status", 
                "Message ID", "Created At", "Updated At"
        );

        List<List<String>> rows = receipts.stream()
                .map(this::receiptToRow)
                .collect(Collectors.toList());

        return generateCsv(headers, rows);
    }

    /**
     * Exports receipt with its lines to CSV format.
     *
     * @param receipt the receipt to export
     * @return CSV content as byte array
     */
    public byte[] exportReceiptWithLines(Receipt receipt) {
        List<String> headers = List.of(
                "Receipt ID", "Doc No", "Line No", "SKU ID", 
                "UOM", "Qty Expected", "SSCC Expected"
        );

        List<List<String>> rows = receipt.getLines().stream()
                .map(line -> receiptLineToRow(receipt, line))
                .collect(Collectors.toList());

        return generateCsv(headers, rows);
    }

    /**
     * Exports pallets to CSV format.
     *
     * @param pallets list of pallets to export
     * @return CSV content as byte array
     */
    public byte[] exportPallets(List<Pallet> pallets) {
        List<String> headers = List.of(
                "ID", "Code", "Code Type", "Status", "Location ID", 
                "SKU ID", "Lot Number", "Expiry Date", "Quantity", "UOM",
                "Receipt ID", "Weight (kg)", "Height (cm)", "Created At"
        );

        List<List<String>> rows = pallets.stream()
                .map(this::palletToRow)
                .collect(Collectors.toList());

        return generateCsv(headers, rows);
    }

    private List<String> receiptToRow(Receipt receipt) {
        List<String> row = new ArrayList<>();
        row.add(toStringValue(receipt.getId()));
        row.add(toStringValue(receipt.getDocNo()));
        row.add(toStringValue(receipt.getDocDate()));
        row.add(toStringValue(receipt.getSupplier()));
        row.add(toStringValue(receipt.getStatus()));
        row.add(toStringValue(receipt.getMessageId()));
        row.add(toStringValue(receipt.getCreatedAt()));
        row.add(toStringValue(receipt.getUpdatedAt()));
        return row;
    }

    private List<String> receiptLineToRow(Receipt receipt, ReceiptLine line) {
        List<String> row = new ArrayList<>();
        row.add(toStringValue(receipt.getId()));
        row.add(toStringValue(receipt.getDocNo()));
        row.add(toStringValue(line.getLineNo()));
        row.add(toStringValue(line.getSkuId()));
        row.add(toStringValue(line.getUom()));
        row.add(toStringValue(line.getQtyExpected()));
        row.add(toStringValue(line.getSsccExpected()));
        return row;
    }

    private List<String> palletToRow(Pallet pallet) {
        List<String> row = new ArrayList<>();
        row.add(toStringValue(pallet.getId()));
        row.add(toStringValue(pallet.getCode()));
        row.add(toStringValue(pallet.getCodeType()));
        row.add(toStringValue(pallet.getStatus()));
        row.add(toStringValue(pallet.getLocation() != null ? pallet.getLocation().getId() : null));
        row.add(toStringValue(pallet.getSkuId()));
        row.add(toStringValue(pallet.getLotNumber()));
        row.add(toStringValue(pallet.getExpiryDate()));
        row.add(toStringValue(pallet.getQuantity()));
        row.add(toStringValue(pallet.getUom()));
        row.add(toStringValue(pallet.getReceipt() != null ? pallet.getReceipt().getId() : null));
        row.add(toStringValue(pallet.getWeightKg()));
        row.add(toStringValue(pallet.getHeightCm()));
        row.add(toStringValue(pallet.getCreatedAt()));
        return row;
    }
}
