package com.wmsdipl.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wmsdipl.desktop.model.Receipt;
import com.wmsdipl.desktop.model.ReceiptLine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;

    public ApiClient() {
        this(System.getenv().getOrDefault("WMS_CORE_API_BASE", "http://localhost:8080"));
    }

    public ApiClient(String baseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.baseUrl = baseUrl;
    }

    public List<Receipt> listReceipts() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/receipts"))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Unexpected status: " + response.statusCode() + " body=" + response.body());
    }

    public Receipt createDraft(String docNo, String supplier) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("docNo", docNo);
        if (supplier != null && !supplier.isBlank()) {
            payload.put("supplier", supplier);
        }
        var lines = mapper.createArrayNode();
        var line = mapper.createObjectNode();
        line.put("lineNo", 1);
        line.put("uom", "PCS");
        line.put("qtyExpected", 0);
        lines.add(line);
        payload.set("lines", lines);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/receipts"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), Receipt.class);
        }
        throw new IOException("Create failed: status=" + response.statusCode() + " body=" + response.body());
    }

    public void confirm(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/confirm");
    }

    public void accept(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/accept");
    }

    public List<ReceiptLine> getReceiptLines(long receiptId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/receipts/" + receiptId + "/lines"))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Lines failed: status=" + response.statusCode() + " body=" + response.body());
    }

    private void postNoBody(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new IOException("Request failed: status=" + response.statusCode() + " body=" + response.body());
    }
}
