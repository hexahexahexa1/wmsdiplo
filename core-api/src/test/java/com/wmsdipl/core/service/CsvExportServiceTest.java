package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CsvExportService.
 * Tests CSV generation, escaping, and export functionality.
 */
class CsvExportServiceTest {

    private CsvExportService csvExportService;

    @BeforeEach
    void setUp() {
        csvExportService = new CsvExportService();
    }

    @Test
    void shouldGenerateCsv_WithHeadersAndRows() {
        // Given
        List<String> headers = Arrays.asList("ID", "Name", "Value");
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("1", "Test", "100"),
                Arrays.asList("2", "Sample", "200")
        );

        // When
        byte[] csv = csvExportService.generateCsv(headers, rows);
        String result = new String(csv, StandardCharsets.UTF_8);

        // Then
        assertNotNull(csv);
        assertTrue(csv.length > 0);
        assertTrue(result.contains("ID,Name,Value"));
        assertTrue(result.contains("1,Test,100"));
        assertTrue(result.contains("2,Sample,200"));
    }

    @Test
    void shouldEscapeCommasInValues() {
        // Given
        List<String> headers = Arrays.asList("Name", "Description");
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("Test", "Value with, comma")
        );

        // When
        byte[] csv = csvExportService.generateCsv(headers, rows);
        String result = new String(csv, StandardCharsets.UTF_8);

        // Then
        assertTrue(result.contains("\"Value with, comma\""));
    }

    @Test
    void shouldEscapeQuotesInValues() {
        // Given
        List<String> headers = Arrays.asList("Name", "Description");
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("Test", "Value with \"quotes\"")
        );

        // When
        byte[] csv = csvExportService.generateCsv(headers, rows);
        String result = new String(csv, StandardCharsets.UTF_8);

        // Then
        assertTrue(result.contains("\"Value with \"\"quotes\"\"\""));
    }

    @Test
    void shouldHandleNullValues() {
        // Given
        List<String> headers = Arrays.asList("ID", "Name", "Value");
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("1", null, "100")
        );

        // When
        byte[] csv = csvExportService.generateCsv(headers, rows);
        String result = new String(csv, StandardCharsets.UTF_8);

        // Then
        assertNotNull(csv);
        assertTrue(result.contains("1,,100"));
    }

    @Test
    void shouldExportReceipts_WhenReceiptsProvided() throws Exception {
        // Given
        Receipt receipt1 = createReceipt(1L, "RCP-001", "SUP-001", ReceiptStatus.DRAFT);
        Receipt receipt2 = createReceipt(2L, "RCP-002", "SUP-002", ReceiptStatus.ACCEPTED);
        List<Receipt> receipts = Arrays.asList(receipt1, receipt2);

        // When
        byte[] csv = csvExportService.exportReceipts(receipts);
        String result = new String(csv, StandardCharsets.UTF_8);

        // Then
        assertNotNull(csv);
        assertTrue(result.contains("ID,Doc No,Doc Date,Supplier,Status"));
        assertTrue(result.contains("RCP-001"));
        assertTrue(result.contains("RCP-002"));
        assertTrue(result.contains("SUP-001"));
        assertTrue(result.contains("SUP-002"));
        assertTrue(result.contains("DRAFT"));
        assertTrue(result.contains("ACCEPTED"));
    }

    @Test
    void shouldExportReceiptWithLines_WhenLinesExist() throws Exception {
        // Given
        Receipt receipt = createReceipt(1L, "RCP-001", "SUP-001", ReceiptStatus.DRAFT);
        ReceiptLine line1 = createReceiptLine(1L, 1, 100L, BigDecimal.TEN);
        ReceiptLine line2 = createReceiptLine(2L, 2, 200L, BigDecimal.valueOf(20));
        receipt.addLine(line1);
        receipt.addLine(line2);

        // When
        byte[] csv = csvExportService.exportReceiptWithLines(receipt);
        String result = new String(csv, StandardCharsets.UTF_8);

        // Then
        assertNotNull(csv);
        assertTrue(result.contains("Receipt ID,Doc No,Line No,SKU ID"));
        assertTrue(result.contains("RCP-001"));
        assertTrue(result.contains("100")); // SKU ID
        assertTrue(result.contains("200")); // SKU ID
        assertTrue(result.contains("10")); // Quantity
        assertTrue(result.contains("20")); // Quantity
    }

    @Test
    void shouldExportPallets_WhenPalletsProvided() throws Exception {
        // Given
        Pallet pallet1 = createPallet(1L, "PLT-001", PalletStatus.EMPTY);
        Pallet pallet2 = createPallet(2L, "PLT-002", PalletStatus.PLACED);
        List<Pallet> pallets = Arrays.asList(pallet1, pallet2);

        // When
        byte[] csv = csvExportService.exportPallets(pallets);
        String result = new String(csv, StandardCharsets.UTF_8);

        // Then
        assertNotNull(csv);
        assertTrue(result.contains("ID,Code,Code Type,Status"));
        assertTrue(result.contains("PLT-001"));
        assertTrue(result.contains("PLT-002"));
        assertTrue(result.contains("EMPTY"));
        assertTrue(result.contains("PLACED"));
    }

    @Test
    void shouldExportEmptyList_WhenNoDataProvided() {
        // Given
        List<String> headers = Arrays.asList("ID", "Name");
        List<List<String>> rows = Arrays.asList();

        // When
        byte[] csv = csvExportService.generateCsv(headers, rows);
        String result = new String(csv, StandardCharsets.UTF_8);

        // Then
        assertNotNull(csv);
        assertTrue(result.contains("ID,Name"));
    }

    @Test
    void shouldIncludeUtf8Bom_ForExcelCompatibility() {
        // Given
        List<String> headers = Arrays.asList("ID");
        List<List<String>> rows = Arrays.asList();

        // When
        byte[] csv = csvExportService.generateCsv(headers, rows);

        // Then
        assertNotNull(csv);
        assertTrue(csv.length >= 3);
        assertEquals((byte) 0xEF, csv[0]);
        assertEquals((byte) 0xBB, csv[1]);
        assertEquals((byte) 0xBF, csv[2]);
    }

    @Test
    void shouldConvertToStringValue_WhenNullProvided() {
        // When
        String result = csvExportService.toStringValue(null);

        // Then
        assertEquals("", result);
    }

    @Test
    void shouldConvertToStringValue_WhenValueProvided() {
        // When
        String result = csvExportService.toStringValue(123);

        // Then
        assertEquals("123", result);
    }

    // Helper methods

    private Receipt createReceipt(Long id, String docNo, String supplier, ReceiptStatus status) throws Exception {
        Receipt receipt = new Receipt();
        receipt.setDocNo(docNo);
        receipt.setDocDate(LocalDate.now());
        receipt.setSupplier(supplier);
        receipt.setStatus(status);

        // Set ID using reflection
        java.lang.reflect.Field idField = Receipt.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(receipt, id);

        return receipt;
    }

    private ReceiptLine createReceiptLine(Long id, int lineNo, Long skuId, BigDecimal qtyExpected) throws Exception {
        ReceiptLine line = new ReceiptLine();
        line.setLineNo(lineNo);
        line.setSkuId(skuId);
        line.setUom("ШТ");
        line.setQtyExpected(qtyExpected);

        // Set ID using reflection
        java.lang.reflect.Field idField = ReceiptLine.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(line, id);

        return line;
    }

    private Pallet createPallet(Long id, String code, PalletStatus status) throws Exception {
        Pallet pallet = new Pallet();
        pallet.setCode(code);
        pallet.setCodeType("INTERNAL");
        pallet.setStatus(status);
        pallet.setSkuId(1L);
        pallet.setQuantity(BigDecimal.TEN);
        pallet.setUom("ШТ");

        // Set ID using reflection
        java.lang.reflect.Field idField = Pallet.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(pallet, id);

        return pallet;
    }
}
