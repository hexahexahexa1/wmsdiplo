package com.wmsdipl.imports.service;

import com.wmsdipl.contracts.dto.ImportPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class ImportClient {

    private static final Logger log = LoggerFactory.getLogger(ImportClient.class);

    private final RestTemplate restTemplate;
    private final String coreApiBase;

    public ImportClient(RestTemplate restTemplate,
                        @Value("${wms.core-api.base-url:http://localhost:8080}") String coreApiBase) {
        this.restTemplate = restTemplate;
        this.coreApiBase = coreApiBase;
    }

    public void send(ImportPayload payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ImportPayload> entity = new HttpEntity<>(payload, headers);
        String url = coreApiBase + "/api/imports";
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("Import sent, status={} payloadMessageId={}", response.getStatusCode(), payload.messageId());
        } catch (HttpClientErrorException.Conflict ex) {
            log.warn("Duplicate import ignored for messageId={}", payload.messageId());
        } catch (HttpClientErrorException ex) {
            log.error("Import failed status={} body={} messageId={}", ex.getStatusCode(), ex.getResponseBodyAsString(), payload.messageId());
            throw ex;
        }
    }
}
