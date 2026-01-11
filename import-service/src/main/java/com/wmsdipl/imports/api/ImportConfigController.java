package com.wmsdipl.imports.api;

import com.wmsdipl.imports.config.ImportScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import/config")
public class ImportConfigController {

    private final ImportScheduler scheduler;

    public ImportConfigController(ImportScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @GetMapping
    public ResponseEntity<ImportConfigResponse> getConfig() {
        String currentFolder = scheduler.getCurrentImportFolder();
        return ResponseEntity.ok(new ImportConfigResponse(currentFolder));
    }

    @PostMapping
    public ResponseEntity<Void> updateFolder(@RequestBody ImportConfigRequest request) {
        scheduler.updateImportFolder(request.folder());
        return ResponseEntity.noContent().build();
    }
}
