package com.wmsdipl.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wmsdipl.desktop.model.Location;
import com.wmsdipl.desktop.model.Pallet;
import com.wmsdipl.desktop.model.PutawayRule;
import com.wmsdipl.desktop.model.Receipt;
import com.wmsdipl.desktop.model.ReceiptLine;
import com.wmsdipl.desktop.model.Scan;
import com.wmsdipl.desktop.model.Sku;
import com.wmsdipl.desktop.model.Task;
import com.wmsdipl.desktop.model.Zone;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private String basicAuth;
    private String currentUsername;

    public ApiClient() {
        this(System.getenv().getOrDefault("WMS_CORE_API_BASE", "http://localhost:8080"));
    }

    public ApiClient(String baseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.baseUrl = baseUrl;
    }

    public boolean login(String username, String password) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("username", username);
        payload.put("password", password);

        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            setCredentials(username, password);
            this.currentUsername = username;
            return true;
        }
        return false;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    private void setCredentials(String username, String password) {
        String token = username + ":" + password;
        this.basicAuth = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public List<Receipt> listReceipts() throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
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

        HttpRequest request = withAuth(HttpRequest.newBuilder())
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

    public void startReceiving(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/start-receiving");
    }

    public void completeReceiving(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/complete-receiving");
    }

    public void startPlacement(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/start-placement");
    }

    public void completePlacement(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/complete-placement");
    }

    public List<ReceiptLine> getReceiptLines(long receiptId) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
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

    public List<Zone> listZones() throws IOException, InterruptedException {
        return getForList("/api/zones", new TypeReference<>() {});
    }

    public List<Location> listLocations() throws IOException, InterruptedException {
        return getForList("/api/locations", new TypeReference<>() {});
    }

    public List<Pallet> listPallets() throws IOException, InterruptedException {
        return getForList("/api/pallets", new TypeReference<>() {});
    }

    public Pallet createPallet(String code) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("code", code);
        payload.put("status", "EMPTY");
        return postForObject("/api/pallets", payload, Pallet.class);
    }

    public List<Task> listTasks(Long receiptId) throws IOException, InterruptedException {
        String path = receiptId == null ? "/api/tasks" : "/api/tasks?receiptId=" + receiptId;
        return getForList(path, new TypeReference<>() {});
    }

    public List<PutawayRule> listPutawayRules() throws IOException, InterruptedException {
        return getForList("/api/putaway-rules", new TypeReference<>() {});
    }

    public List<Sku> listSkus() throws IOException, InterruptedException {
        return getForList("/api/skus", new TypeReference<>() {});
    }

    public Sku createSku(String code, String name, String uom) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("code", code);
        payload.put("name", name);
        payload.put("uom", uom);
        return postForObject("/api/skus", payload, Sku.class);
    }

    public void deleteSku(Long id) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/skus/" + id))
            .DELETE()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new IOException("Delete SKU failed: status=" + response.statusCode() + " body=" + response.body());
    }

    // Terminal lifecycle methods
    public Task assignTask(Long taskId, String assignee) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("assignee", assignee);
        return postForObject("/api/tasks/" + taskId + "/assign", payload, Task.class);
    }

    public Task startTask(Long taskId) throws IOException, InterruptedException {
        return postForObject("/api/tasks/" + taskId + "/start", null, Task.class);
    }

    public Task completeTask(Long taskId) throws IOException, InterruptedException {
        return postForObject("/api/tasks/" + taskId + "/complete", null, Task.class);
    }

    public Task releaseTask(Long taskId) throws IOException, InterruptedException {
        return postForObject("/api/tasks/" + taskId + "/release", null, Task.class);
    }

    public Scan recordScan(Long taskId, String palletCode, String barcode, Integer qty, String comment) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("palletCode", palletCode);
        payload.put("qty", qty);
        if (barcode != null && !barcode.isBlank()) {
            payload.put("barcode", barcode);
        }
        if (comment != null && !comment.isBlank()) {
            payload.put("comment", comment);
        }
        return postForObject("/api/tasks/" + taskId + "/scans", payload, Scan.class);
    }

    public List<Scan> getTaskScans(Long taskId) throws IOException, InterruptedException {
        return getForList("/api/tasks/" + taskId + "/scans", new TypeReference<>() {});
    }

    public Task getTask(Long taskId) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/tasks/" + taskId))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), Task.class);
        }
        throw new IOException("Get task failed: status=" + response.statusCode() + " body=" + response.body());
    }

    public boolean hasDiscrepancies(Long taskId) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/tasks/" + taskId + "/has-discrepancies"))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), Boolean.class);
        }
        throw new IOException("Check discrepancies failed: status=" + response.statusCode() + " body=" + response.body());
    }

    public List<Task> getAllTasks() throws IOException, InterruptedException {
        return getForList("/api/tasks", new TypeReference<>() {});
    }

    private void postNoBody(String path) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
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

    private <T> T postForObject(String path, Object payload, Class<T> responseType) throws IOException, InterruptedException {
        HttpRequest.Builder builder = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json");
        
        if (payload == null) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));
        }
        
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), responseType);
        }
        throw new IOException("Request failed: status=" + response.statusCode() + " body=" + response.body());
    }

    private <T> List<T> getForList(String path, TypeReference<List<T>> type) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + path))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), type);
        }
        throw new IOException("Unexpected status: " + response.statusCode() + " body=" + response.body());
    }

    private HttpRequest.Builder withAuth(HttpRequest.Builder builder) {
        if (basicAuth != null && !basicAuth.isBlank()) {
            builder.header("Authorization", "Basic " + basicAuth);
        }
        return builder;
    }
}
