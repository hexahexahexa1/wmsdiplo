package com.wmsdipl.imports.service;

import com.wmsdipl.contracts.dto.ImportPayload;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class XmlParserTest {

    private final XmlParser xmlParser = new XmlParser();

    @Test
    void shouldParseLotNumberAndExpiryDate_WhenPresentInLineAttributes() throws Exception {
        Path xmlFile = Files.createTempFile("receipt-", ".xml");
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <receipt messageId="msg-001" docNo="DOC-001" docDate="2026-02-08" supplier="SUP1">
              <line lineNo="1" sku="SKU-1" name="Product 1" uom="PCS" qtyExpected="10" sscc="SSCC-1" lotNumber="LOT-100" expiryDate="2026-12-31"/>
            </receipt>
            """;
        Files.writeString(xmlFile, xml, StandardCharsets.UTF_8);

        try {
            ImportPayload payload = xmlParser.parse(xmlFile.toFile());
            assertNotNull(payload);
            assertEquals(1, payload.lines().size());
            ImportPayload.Line line = payload.lines().get(0);
            assertEquals("LOT-100", line.lotNumber());
            assertEquals(LocalDate.of(2026, 12, 31), line.expiryDate());
        } finally {
            Files.deleteIfExists(xmlFile);
        }
    }

    @Test
    void shouldSetNullLotNumberAndExpiryDate_WhenAttributesMissing() throws Exception {
        Path xmlFile = Files.createTempFile("receipt-", ".xml");
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <receipt messageId="msg-002" docNo="DOC-002">
              <line lineNo="1" sku="SKU-2" uom="PCS" qtyExpected="5"/>
            </receipt>
            """;
        Files.writeString(xmlFile, xml, StandardCharsets.UTF_8);

        try {
            ImportPayload payload = xmlParser.parse(xmlFile.toFile());
            assertNotNull(payload);
            assertEquals(1, payload.lines().size());
            ImportPayload.Line line = payload.lines().get(0);
            assertNull(line.lotNumber());
            assertNull(line.expiryDate());
        } finally {
            Files.deleteIfExists(xmlFile);
        }
    }
}
