package com.wmsdipl.imports.service;

import com.wmsdipl.contracts.dto.ImportPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class ImportHandler {
    private static final Logger log = LoggerFactory.getLogger(ImportHandler.class);

    private final XmlParser xmlParser;
    private final ImportClient importClient;

    public ImportHandler(XmlParser xmlParser, ImportClient importClient) {
        this.xmlParser = xmlParser;
        this.importClient = importClient;
    }

    public ImportResult handle(File file) {
        log.info("Received file: {} (size={} bytes)", file.getName(), file.length());
        try {
            ImportPayload payload = xmlParser.parse(file);
            importClient.send(payload);
            log.info("Processed file {} messageId={}", file.getName(), payload.messageId());
            return new ImportResult(true, null);
        } catch (Exception ex) {
            log.error("Failed to process file {}: {}", file.getName(), ex.getMessage(), ex);
            return new ImportResult(false, ex.getMessage());
        }
    }
}
