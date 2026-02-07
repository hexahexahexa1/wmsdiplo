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
import com.wmsdipl.desktop.model.SkuUnitConfig;
import com.wmsdipl.desktop.model.StockItem;
import com.wmsdipl.desktop.model.StockMovement;
import com.wmsdipl.desktop.model.Task;
import com.wmsdipl.desktop.model.User;
import com.wmsdipl.desktop.model.Zone;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private String basicAuth;
    private String currentUsername;
    private User currentUser;

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

    public User login(String username, String password) throws IOException, InterruptedException {
        System.out.println("[DEBUG] ApiClient.login: Attempting login for user: " + username);
        var payload = mapper.createObjectNode();
        payload.put("username", username);
        payload.put("password", password);

        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[DEBUG] ApiClient.login: Response status: " + response.statusCode());
        if (response.statusCode() == 200) {
            setCredentials(username, password);
            this.currentUsername = username;
            this.currentUser = mapper.readValue(response.body(), User.class);
            System.out.println("[DEBUG] ApiClient.login: Login successful, credentials saved. Role: " + currentUser.role());
            return currentUser;
        }
        System.out.println("[WARNING] ApiClient.login: Login failed with status " + response.statusCode() + ", body: " + response.body());
        return null;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void logout() {
        this.basicAuth = null;
        this.currentUsername = null;
        this.currentUser = null;
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
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/receipts", response.body()));
    }

    public Receipt createDraft(
        String docNo,
        LocalDate docDate,
        String supplier,
        boolean crossDock,
        String outboundRef
    ) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("docNo", docNo);
        if (docDate != null) {
            payload.put("docDate", docDate.toString());
        }
        if (supplier != null && !supplier.isBlank()) {
            payload.put("supplier", supplier);
        }
        payload.put("crossDock", crossDock);
        if (outboundRef != null && !outboundRef.isBlank()) {
            payload.put("outboundRef", outboundRef);
        }

        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/receipts/drafts"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), Receipt.class);
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/receipts/drafts", response.body()));
    }

    public ReceiptLine addReceiptLine(
        long receiptId,
        Integer lineNo,
        long skuId,
        String uom,
        BigDecimal qtyExpected,
        String ssccExpected
    ) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        if (lineNo != null) {
            payload.put("lineNo", lineNo);
        }
        payload.put("skuId", skuId);
        payload.put("uom", uom);
        payload.put("qtyExpected", qtyExpected);
        if (ssccExpected != null && !ssccExpected.isBlank()) {
            payload.put("ssccExpected", ssccExpected);
        }

        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/receipts/" + receiptId + "/lines"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), ReceiptLine.class);
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/receipts/" + receiptId + "/lines", response.body()));
    }

    public ReceiptLine updateReceiptLine(
        long receiptId,
        long lineId,
        Integer lineNo,
        long skuId,
        String uom,
        BigDecimal qtyExpected,
        String ssccExpected
    ) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        if (lineNo != null) {
            payload.put("lineNo", lineNo);
        }
        payload.put("skuId", skuId);
        payload.put("uom", uom);
        payload.put("qtyExpected", qtyExpected);
        if (ssccExpected != null && !ssccExpected.isBlank()) {
            payload.put("ssccExpected", ssccExpected);
        }

        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/receipts/" + receiptId + "/lines/" + lineId))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), ReceiptLine.class);
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/receipts/" + receiptId + "/lines/" + lineId, response.body()));
    }

    public void deleteReceiptLine(long receiptId, long lineId) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/receipts/" + receiptId + "/lines/" + lineId))
            .DELETE()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/receipts/" + receiptId + "/lines/" + lineId, response.body()));
    }

    public void confirm(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/confirm");
    }

    public void accept(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/accept");
    }

    public Integer startReceiving(long id) throws IOException, InterruptedException {
        Map<String, Integer> res = postForObject("/api/receipts/" + id + "/start-receiving", null, new TypeReference<Map<String, Integer>>(){});
        return res.get("count");
    }

    public void completeReceiving(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/complete-receiving");
    }

    public Integer startPlacement(long id) throws IOException, InterruptedException {
        Map<String, Integer> res = postForObject("/api/receipts/" + id + "/start-placement", null, new TypeReference<Map<String, Integer>>(){});
        return res.get("count");
    }

    public void completePlacement(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/complete-placement");
    }

    public Integer startShipping(long id) throws IOException, InterruptedException {
        Map<String, Integer> res = postForObject("/api/receipts/" + id + "/start-shipping", null, new TypeReference<Map<String, Integer>>(){});
        return res.get("count");
    }

    public void completeShipping(long id) throws IOException, InterruptedException {
        postNoBody("/api/receipts/" + id + "/complete-shipping");
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
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/receipts/" + receiptId + "/lines", response.body()));
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

    public List<Task> listTasksFiltered(
            String assignee,
            String status,
            String taskType,
            Long receiptId,
            int page,
            int size,
            String sort
    ) throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/tasks?page=" + page + "&size=" + size);
        if (assignee != null && !assignee.isBlank()) {
            path.append("&assignee=").append(encode(assignee));
        }
        if (status != null && !status.isBlank()) {
            path.append("&status=").append(encode(status));
        }
        if (taskType != null && !taskType.isBlank()) {
            path.append("&taskType=").append(encode(taskType));
        }
        if (receiptId != null) {
            path.append("&receiptId=").append(receiptId);
        }
        if (sort != null && !sort.isBlank()) {
            path.append("&sort=").append(encode(sort));
        }

        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + path))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            var root = mapper.readTree(response.body());
            var content = root.get("content");
            if (content == null || !content.isArray()) {
                return List.of();
            }
            return mapper.readValue(content.toString(), new TypeReference<>() {});
        }
        throw new IOException(formatErrorMessage(response.statusCode(), path.toString(), response.body()));
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

    public List<SkuUnitConfig> listSkuUnitConfigs(Long skuId) throws IOException, InterruptedException {
        return getForList("/api/skus/" + skuId + "/unit-configs", new TypeReference<>() {});
    }

    public List<SkuUnitConfig> replaceSkuUnitConfigs(Long skuId, List<SkuUnitConfig> configs)
            throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        var array = mapper.createArrayNode();
        for (SkuUnitConfig config : configs) {
            var node = mapper.createObjectNode();
            if (config.id() != null) {
                node.put("id", config.id());
            }
            node.put("unitCode", config.unitCode());
            node.put("factorToBase", config.factorToBase());
            node.put("unitsPerPallet", config.unitsPerPallet());
            node.put("isBase", Boolean.TRUE.equals(config.isBase()));
            node.put("active", Boolean.TRUE.equals(config.active()));
            array.add(node);
        }
        payload.set("configs", array);

        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/skus/" + skuId + "/unit-configs"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Replace SKU unit configs failed: status=" + response.statusCode() + " body=" + response.body());
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

    public Scan recordScan(Long taskId, String palletCode, String barcode, Integer qty, String comment, String locationCode,
                           Boolean damageFlag, String damageType, String damageDescription, 
                           String lotNumber, String expiryDate) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("requestId", java.util.UUID.randomUUID().toString());
        payload.put("palletCode", palletCode);
        payload.put("qty", qty);
        if (barcode != null && !barcode.isBlank()) {
            payload.put("barcode", barcode);
        }
        if (comment != null && !comment.isBlank()) {
            payload.put("comment", comment);
        }
        if (locationCode != null && !locationCode.isBlank()) {
            payload.put("locationCode", locationCode);
        }
        // Damage tracking fields
        if (damageFlag != null && damageFlag) {
            payload.put("damageFlag", true);
            if (damageType != null && !damageType.isBlank()) {
                payload.put("damageType", mapDamageType(damageType));
            }
            if (damageDescription != null && !damageDescription.isBlank()) {
                payload.put("damageDescription", damageDescription);
            }
        }
        // Lot tracking fields
        if (lotNumber != null && !lotNumber.isBlank()) {
            payload.put("lotNumber", lotNumber);
        }
        if (expiryDate != null && !expiryDate.isBlank()) {
            payload.put("expiryDate", expiryDate);
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

    // Zone CRUD operations
    public Zone createZone(String code, String name, Integer priorityRank, String description) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("code", code);
        payload.put("name", name);
        if (priorityRank != null) {
            payload.put("priorityRank", priorityRank);
        }
        if (description != null && !description.isBlank()) {
            payload.put("description", description);
        }
        return postForObject("/api/zones", payload, Zone.class);
    }

    public Zone updateZone(Long id, String code, String name, Integer priorityRank, String description, Boolean active) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        if (code != null) payload.put("code", code);
        if (name != null) payload.put("name", name);
        if (priorityRank != null) payload.put("priorityRank", priorityRank);
        if (description != null) payload.put("description", description);
        if (active != null) payload.put("active", active);
        
        return putForObject("/api/zones/" + id, payload, Zone.class);
    }

    public void deleteZone(Long id) throws IOException, InterruptedException {
        deleteResource("/api/zones/" + id);
    }

    // Location CRUD operations
    public Location createLocation(Long zoneId, String code, String locationType, String aisle, String bay, String level, Integer maxPallets) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("zoneId", zoneId);
        payload.put("code", code);
        if (locationType != null && !locationType.isBlank()) payload.put("locationType", locationType);
        if (aisle != null && !aisle.isBlank()) payload.put("aisle", aisle);
        if (bay != null && !bay.isBlank()) payload.put("bay", bay);
        if (level != null && !level.isBlank()) payload.put("level", level);
        if (maxPallets != null) payload.put("maxPallets", maxPallets);
        
        return postForObject("/api/locations", payload, Location.class);
    }

    public Location updateLocation(Long id, Long zoneId, String code, String locationType, String aisle, String bay, String level, Integer maxPallets, String status, Boolean active) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        if (zoneId != null) payload.put("zoneId", zoneId);
        if (code != null) payload.put("code", code);
        if (locationType != null) payload.put("locationType", locationType);
        if (aisle != null) payload.put("aisle", aisle);
        if (bay != null) payload.put("bay", bay);
        if (level != null) payload.put("level", level);
        if (maxPallets != null) payload.put("maxPallets", maxPallets);
        if (status != null) payload.put("status", status);
        if (active != null) payload.put("active", active);
        
        return putForObject("/api/locations/" + id, payload, Location.class);
    }

    public void deleteLocation(Long id) throws IOException, InterruptedException {
        deleteResource("/api/locations/" + id);
    }

    public Location blockLocation(Long id) throws IOException, InterruptedException {
        return postForObject("/api/locations/" + id + "/block", null, Location.class);
    }

    public Location unblockLocation(Long id) throws IOException, InterruptedException {
        return postForObject("/api/locations/" + id + "/unblock", null, Location.class);
    }

    // User CRUD operations
    public List<User> listUsers() throws IOException, InterruptedException {
        return getForList("/api/users", new TypeReference<>() {});
    }

    public User getUser(Long id) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/users/" + id))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), User.class);
        }
        throw new IOException("Get user failed: status=" + response.statusCode() + " body=" + response.body());
    }

    public User createUser(String username, String password, String fullName, String email, String role, Boolean active) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("username", username);
        payload.put("password", password);
        if (fullName != null && !fullName.isBlank()) payload.put("fullName", fullName);
        if (email != null && !email.isBlank()) payload.put("email", email);
        payload.put("role", role);
        if (active != null) payload.put("active", active);
        
        return postForObject("/api/users", payload, User.class);
    }

    public User updateUser(Long id, String fullName, String email, String role, Boolean active) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        if (fullName != null) payload.put("fullName", fullName);
        if (email != null) payload.put("email", email);
        if (role != null) payload.put("role", role);
        if (active != null) payload.put("active", active);
        
        return putForObject("/api/users/" + id, payload, User.class);
    }

    public void changePassword(Long id, String newPassword) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("password", newPassword);
        
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/users/" + id + "/password"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/users/" + id + "/password", response.body()));
    }

    public void deleteUser(Long id) throws IOException, InterruptedException {
        deleteResource("/api/users/" + id);
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
        throw new IOException(formatErrorMessage(response.statusCode(), path, response.body()));
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
        
        throw new IOException(formatErrorMessage(response.statusCode(), path, response.body()));
    }

    private <T> T postForObject(String path, Object payload, TypeReference<T> responseType) throws IOException, InterruptedException {
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
        
        throw new IOException(formatErrorMessage(response.statusCode(), path, response.body()));
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
        throw new IOException(formatErrorMessage(response.statusCode(), path, response.body()));
    }

    private <T> T putForObject(String path, Object payload, Class<T> responseType) throws IOException, InterruptedException {
        HttpRequest.Builder builder = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json");
        
        if (payload == null) {
            builder.PUT(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));
        }
        
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), responseType);
        }
        throw new IOException(formatErrorMessage(response.statusCode(), path, response.body()));
    }

    private void deleteResource(String path) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + path))
            .DELETE()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new IOException(formatErrorMessage(response.statusCode(), path, response.body()));
    }
    
    private String formatErrorMessage(int statusCode, String path, String body) {
        System.out.println("[ERROR] ApiClient: HTTP " + statusCode + " for " + path);
        System.out.println("[ERROR] ApiClient: Response body: " + (body == null || body.isEmpty() ? "<empty>" : body));
        
        String resourceName = extractResourceName(path);
        
        switch (statusCode) {
            case 403:
                return "Доступ запрещен: у текущего пользователя недостаточно прав для выполнения операции '" 
                    + resourceName + "'. Требуется роль ADMIN.";
            case 404:
                String bodyMsg = extractErrorFromBody(body);
                if (bodyMsg != null && !bodyMsg.isEmpty() && !bodyMsg.equals(body)) {
                    return bodyMsg;
                }
                return "Ресурс не найден: " + resourceName;
            case 400:
                return "Неверный запрос: " + extractErrorFromBody(body);
            case 401:
                return "Требуется авторизация. Пожалуйста, войдите в систему заново.";
            case 409:
                // Conflict - try to extract message from body, often contains business logic errors
                String conflictMessage = extractErrorFromBody(body);
                return conflictMessage.isEmpty() ? "Конфликт данных при выполнении операции" : conflictMessage;
            case 500:
                return "Внутренняя ошибка сервера при выполнении операции '" + resourceName + "'";
            default:
                return "Ошибка " + statusCode + ": " + resourceName + " - " + body;
        }
    }
    
    private String extractResourceName(String path) {
        // Extract user-friendly resource name from path
        if (path.contains("/users")) return "управление пользователями";
        if (path.contains("/locations")) return "управление ячейками";
        if (path.contains("/zones")) return "управление зонами";
        if (path.contains("/receipts")) return "управление приемками";
        if (path.contains("/tasks")) return "управление задачами";
        if (path.contains("/pallets")) return "управление паллетами";
        if (path.contains("/skus")) return "управление номенклатурой";
        return path;
    }
    
    private String extractErrorFromBody(String body) {
        try {
            // Try to extract error message from JSON body
            var jsonNode = mapper.readTree(body);
            // Spring Boot ResponseStatusException can use "message", "error", or "reason"
            if (jsonNode.has("message")) {
                return jsonNode.get("message").asText();
            }
            if (jsonNode.has("reason")) {
                return jsonNode.get("reason").asText();
            }
            if (jsonNode.has("error")) {
                return jsonNode.get("error").asText();
            }
        } catch (Exception e) {
            // If parsing fails, return raw body
        }
        return body;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String mapDamageType(String value) {
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "PHYSICAL", "PHYSICAL_DAMAGE" -> "PHYSICAL_DAMAGE";
            case "WATER", "WATER_DAMAGE" -> "WATER_DAMAGE";
            case "EXPIRED" -> "EXPIRED";
            case "TEMPERATURE_ABUSE" -> "TEMPERATURE_ABUSE";
            case "CONTAMINATION" -> "CONTAMINATION";
            default -> "OTHER";
        };
    }

    // Stock Inventory API methods
    
    /**
     * Get paginated stock inventory with optional filters.
     * 
     * @param skuCode SKU code filter (optional)
     * @param locationCode Location code filter (optional)
     * @param palletBarcode Pallet barcode filter (partial match, optional)
     * @param receiptId Receipt ID filter (optional)
     * @param status Pallet status filter (optional)
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paginated stock items
     */
    public StockPage listStock(String skuCode, String locationCode, String palletBarcode, 
                               Long receiptId, String status, int page, int size) 
            throws IOException, InterruptedException {
        StringBuilder url = new StringBuilder(baseUrl + "/api/stock?page=" + page + "&size=" + size);
        
        if (skuCode != null && !skuCode.isBlank()) {
            url.append("&skuCode=").append(skuCode);
        }
        if (locationCode != null && !locationCode.isBlank()) {
            url.append("&locationCode=").append(locationCode);
        }
        if (palletBarcode != null && !palletBarcode.isBlank()) {
            url.append("&palletBarcode=").append(palletBarcode);
        }
        if (receiptId != null) {
            url.append("&receiptId=").append(receiptId);
        }
        if (status != null && !status.isBlank()) {
            url.append("&status=").append(status);
        }
        
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(url.toString()))
            .GET()
            .header("Accept", "application/json")
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), StockPage.class);
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/stock", response.body()));
    }
    
    /**
     * Get single pallet stock details.
     */
    public StockItem getStockItem(Long palletId) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/stock/pallet/" + palletId))
            .GET()
            .header("Accept", "application/json")
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), StockItem.class);
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/stock/pallet/" + palletId, response.body()));
    }
    
    /**
     * Get movement history for a specific pallet.
     */
    public List<StockMovement> getPalletHistory(Long palletId) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/stock/pallet/" + palletId + "/history"))
            .GET()
            .header("Accept", "application/json")
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/stock/pallet/" + palletId + "/history", response.body()));
    }
    
    /**
     * Get stock items by location.
     */
    public StockPage getStockByLocation(Long locationId, int page, int size) throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/stock/location/" + locationId + "?page=" + page + "&size=" + size))
            .GET()
            .header("Accept", "application/json")
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), StockPage.class);
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/stock/location/" + locationId, response.body()));
    }
    
    /**
     * Stock page wrapper for pagination support.
     */
    public static class StockPage {
        public List<StockItem> content;
        public int totalPages;
        public long totalElements;
        public int number;
        public int size;
        
        public StockPage() {}
    }

    // ========== Bulk Operations API ==========

    /**
     * Bulk assign tasks to operator.
     */
    public Map<String, Object> bulkAssignTasks(List<Long> taskIds, String assignee) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        var taskIdsArray = mapper.createArrayNode();
        taskIds.forEach(taskIdsArray::add);
        payload.set("taskIds", taskIdsArray);
        payload.put("assignee", assignee);
        
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/bulk/assign"))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Bulk assign failed: status=" + response.statusCode() + " body=" + response.body());
    }

    /**
     * Bulk set priority for tasks.
     */
    public Map<String, Object> bulkSetPriority(List<Long> taskIds, Integer priority) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        var taskIdsArray = mapper.createArrayNode();
        taskIds.forEach(taskIdsArray::add);
        payload.set("taskIds", taskIdsArray);
        payload.put("priority", priority);
        
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/bulk/priority"))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Bulk set priority failed: status=" + response.statusCode() + " body=" + response.body());
    }

    /**
     * Bulk create pallets.
     */
    public Map<String, Object> bulkCreatePallets(String prefix, Integer startNumber, Integer count) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        payload.put("prefix", prefix);
        payload.put("startNumber", startNumber);
        payload.put("count", count);
        
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/bulk/pallets"))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Bulk create pallets failed: status=" + response.statusCode() + " body=" + response.body());
    }

    /**
     * Bulk cancel tasks.
     */
    public Map<String, Object> bulkCancelTasks(List<Long> taskIds) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode();
        var taskIdsArray = mapper.createArrayNode();
        taskIds.forEach(taskIdsArray::add);
        payload.set("taskIds", taskIdsArray);
        
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/bulk/cancel"))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Bulk cancel tasks failed: status=" + response.statusCode() + " body=" + response.body());
    }

    // ========== Analytics API ==========

    /**
     * Get receiving analytics for date range.
     */
    public Map<String, Object> getReceivingAnalytics(String fromDate, String toDate) throws IOException, InterruptedException {
        String url = "/api/analytics/receiving";
        if (fromDate != null && toDate != null) {
            url += "?fromDate=" + fromDate + "&toDate=" + toDate;
        }
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + url))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Get analytics failed: status=" + response.statusCode() + " body=" + response.body());
    }

    /**
     * Get receiving analytics for date range.
     */
    public Map<String, Object> getReceivingAnalytics(LocalDate fromDate, LocalDate toDate) throws IOException, InterruptedException {
        String path = "/api/analytics/receiving?fromDate=" + fromDate + "&toDate=" + toDate;
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + path))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/analytics/receiving", response.body()));
    }

    public Map<String, Object> getReceivingHealth(LocalDate fromDate, LocalDate toDate, int thresholdHours) throws IOException, InterruptedException {
        String path = "/api/analytics/receiving-health?fromDate=" + fromDate + "&toDate=" + toDate + "&thresholdHours=" + thresholdHours;
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + path))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException(formatErrorMessage(response.statusCode(), "/api/analytics/receiving-health", response.body()));
    }

    /**
     * Get today's receiving analytics.
     */
    public Map<String, Object> getTodayAnalytics() throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/analytics/receiving/today"))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Get today analytics failed: status=" + response.statusCode() + " body=" + response.body());
    }

    /**
     * Get this week's receiving analytics.
     */
    public Map<String, Object> getWeekAnalytics() throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/analytics/receiving/week"))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Get week analytics failed: status=" + response.statusCode() + " body=" + response.body());
    }

    /**
     * Get this month's receiving analytics.
     */
    public Map<String, Object> getMonthAnalytics() throws IOException, InterruptedException {
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + "/api/analytics/receiving/month"))
            .GET()
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), new TypeReference<>() {});
        }
        throw new IOException("Get month analytics failed: status=" + response.statusCode() + " body=" + response.body());
    }

    /**
     * Export receiving analytics to CSV for date range.
     */
    public byte[] exportReceivingAnalyticsCsv(LocalDate fromDate, LocalDate toDate) throws IOException, InterruptedException {
        String path = "/api/analytics/export-csv?fromDate=" + fromDate + "&toDate=" + toDate;
        HttpRequest request = withAuth(HttpRequest.newBuilder())
            .uri(URI.create(baseUrl + path))
            .GET()
            .header("Accept", "text/csv")
            .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        throw new IOException("Export analytics failed: status=" + response.statusCode());
    }

    private HttpRequest.Builder withAuth(HttpRequest.Builder builder) {
        if (basicAuth != null && !basicAuth.isBlank()) {
            builder.header("Authorization", "Basic " + basicAuth);
            System.out.println("[DEBUG] ApiClient: Adding Authorization header (basicAuth set)");
        } else {
            System.out.println("[WARNING] ApiClient: No credentials available for authentication!");
        }
        return builder;
    }
}
