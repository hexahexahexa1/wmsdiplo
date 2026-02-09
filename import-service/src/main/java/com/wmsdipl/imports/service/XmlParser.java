package com.wmsdipl.imports.service;

import com.wmsdipl.contracts.dto.ImportPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class XmlParser {

    private static final Logger log = LoggerFactory.getLogger(XmlParser.class);

    public ImportPayload parse(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setIgnoringComments(true);
        
        // Защита от XXE (XML External Entity) атак
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        
        Document doc = factory.newDocumentBuilder().parse(file);
        Element root = doc.getDocumentElement();

        String messageId = root.getAttribute("messageId");
        if (messageId == null || messageId.isBlank()) {
            messageId = hashFile(file);
        }
        String docNo = root.getAttribute("docNo");
        String docDateAttr = root.getAttribute("docDate");
        LocalDate docDate = docDateAttr == null || docDateAttr.isBlank() ? null : LocalDate.parse(docDateAttr);
        String supplier = root.getAttribute("supplier");
        String crossDockAttr = root.getAttribute("crossDock");
        Boolean crossDock = crossDockAttr == null || crossDockAttr.isBlank() ? null : Boolean.parseBoolean(crossDockAttr);
        String outboundRef = root.getAttribute("outboundRef");
        if (outboundRef != null && outboundRef.isBlank()) {
            outboundRef = null;
        }

        List<ImportPayload.Line> lines = new ArrayList<>();
        NodeList lineNodes = root.getElementsByTagName("line");
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element lineEl = (Element) lineNodes.item(i);
            ImportPayload.Line line = new ImportPayload.Line(
                parseInt(lineEl.getAttribute("lineNo")),
                lineEl.getAttribute("sku"),
                lineEl.getAttribute("name"),
                lineEl.getAttribute("uom"),
                parseDecimal(lineEl.getAttribute("qtyExpected")),
                lineEl.getAttribute("packaging"),
                lineEl.getAttribute("sscc"),
                parseText(lineEl.getAttribute("lotNumber")),
                parseDate(lineEl.getAttribute("expiryDate"))
            );
            lines.add(line);
        }

        log.info("Parsed XML docNo={} lines={}", docNo, lines.size());
        return new ImportPayload(messageId, docNo, docDate, supplier, crossDock, outboundRef, lines);
    }

    private String hashFile(File file) throws Exception {
        byte[] content = Files.readAllBytes(file.toPath());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content);
        return HexFormat.of().formatHex(hash);
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value);
    }

    private String parseText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }
}
