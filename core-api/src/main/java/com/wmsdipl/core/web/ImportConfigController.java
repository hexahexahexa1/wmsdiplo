package com.wmsdipl.core.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/import/config")
@Tag(name = "Import Configuration", description = "Configuration management for import service")
public class ImportConfigController {

    private final JdbcTemplate jdbcTemplate;

    public ImportConfigController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @Operation(summary = "Get import configuration", description = "Returns current import folder path and API URL")
    public ResponseEntity<Map<String, String>> getConfig() {
        Map<String, String> config = new HashMap<>();
        
        String importFolder = jdbcTemplate.queryForObject(
            "SELECT config_value FROM import_config WHERE config_key = 'import_folder'",
            String.class
        );
        
        String apiUrl = jdbcTemplate.queryForObject(
            "SELECT config_value FROM import_config WHERE config_key = 'api_url'",
            String.class
        );
        
        config.put("importFolder", importFolder);
        config.put("apiUrl", apiUrl);
        
        return ResponseEntity.ok(config);
    }

    @PostMapping("/folder")
    @Operation(summary = "Update import folder", description = "Updates the folder path where XML files are monitored for import")
    public ResponseEntity<Map<String, String>> updateImportFolder(@RequestBody Map<String, String> request) {
        String folder = request.get("folder");
        
        if (folder == null || folder.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Folder path cannot be empty"));
        }
        
        jdbcTemplate.update(
            "INSERT INTO import_config (config_key, config_value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value, updated_at = CURRENT_TIMESTAMP",
            "import_folder", folder
        );
        
        return ResponseEntity.ok(Map.of("importFolder", folder, "message", "Import folder updated successfully"));
    }

    @PostMapping("/api-url")
    @Operation(summary = "Update API URL", description = "Updates the core API URL used by import service")
    public ResponseEntity<Map<String, String>> updateApiUrl(@RequestBody Map<String, String> request) {
        String apiUrl = request.get("apiUrl");
        
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "API URL cannot be empty"));
        }
        
        jdbcTemplate.update(
            "INSERT INTO import_config (config_key, config_value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value, updated_at = CURRENT_TIMESTAMP",
            "api_url", apiUrl
        );
        
        return ResponseEntity.ok(Map.of("apiUrl", apiUrl, "message", "API URL updated successfully"));
    }
}
