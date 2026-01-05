package com.wmsdipl.imports.config;

import com.wmsdipl.imports.service.ImportHandler;
import com.wmsdipl.imports.service.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Configuration
@EnableScheduling
public class ImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImportScheduler.class);

    private volatile Path importFolder;
    private volatile Path loadedFolder;
    private volatile Path errorFolder;
    private final ImportHandler importHandler;
    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    public ImportScheduler(@Value("${wms.import.folder:input}") String importFolder,
                           ImportHandler importHandler) {
        setImportFolderInternal(importFolder);
        this.importHandler = importHandler;
    }

    public synchronized void updateImportFolder(String newFolder) {
        setImportFolderInternal(newFolder);
        processed.clear();
        log.info("Import folder switched to {}", this.importFolder.toAbsolutePath());
    }

    private void setImportFolderInternal(String folder) {
        this.importFolder = Paths.get(folder);
        this.loadedFolder = this.importFolder.resolve("loaded");
        this.errorFolder = this.importFolder.resolve("error");
    }

    @Scheduled(fixedDelayString = "${wms.import.poll-interval-ms:2000}")
    public void pollFolder() {
        try {
            if (!Files.exists(importFolder)) {
                Files.createDirectories(importFolder);
            }
            Files.createDirectories(loadedFolder);
            Files.createDirectories(errorFolder);
            try (Stream<Path> stream = Files.list(importFolder)) {
                stream.filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                    .filter(Files::isRegularFile)
                    .filter(p -> processed.add(p.toString()))
                    .forEach(this::processFile);
            }
        } catch (Exception e) {
            log.error("Watcher error: {}", e.getMessage(), e);
        }
    }

    private void processFile(Path path) {
        File file = path.toFile();
        try {
            ImportResult result = importHandler.handle(file);
            Path targetFolder = result.success() ? loadedFolder : errorFolder;
            Files.createDirectories(targetFolder);
            Path target = targetFolder.resolve(file.getName());
            Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
            if (!result.success()) {
                Path errorLog = targetFolder.resolve(file.getName() + ".error.log");
                String message = result.errorMessage() == null ? "Unknown error" : result.errorMessage();
                Files.writeString(errorLog, message, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            log.info("File {} moved to {}", file.getName(), targetFolder.toAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to move file {}: {}", file.getName(), e.getMessage(), e);
        } finally {
            processed.remove(path.toString());
        }
    }
}
