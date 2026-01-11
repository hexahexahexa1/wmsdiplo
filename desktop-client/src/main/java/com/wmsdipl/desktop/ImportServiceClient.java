package com.wmsdipl.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ImportServiceClient {
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;

    public ImportServiceClient() {
        this(System.getenv().getOrDefault("WMS_IMPORT_BASE", "http://localhost:8090"));
    }

    public ImportServiceClient(String baseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.baseUrl = baseUrl;
    }

    public String getCurrentFolder() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/import/config"))
            .header("Accept", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            Map<String, Object> config = mapper.readValue(response.body(), Map.class);
            return (String) config.get("folder");
        }
        throw new IOException("Get config failed: status=" + response.statusCode() + " body=" + response.body());
    }

    public void updateFolder(String folderPath) throws IOException, InterruptedException {
        var body = mapper.writeValueAsString(Map.of("folder", folderPath));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/import/config"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new IOException("Update folder failed: status=" + response.statusCode() + " body=" + response.body());
    }
}
