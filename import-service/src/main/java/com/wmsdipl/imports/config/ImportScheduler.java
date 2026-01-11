package com.wmsdipl.imports.config;

import com.wmsdipl.imports.domain.ImportConfig;
import com.wmsdipl.imports.repository.ImportConfigRepository;
import com.wmsdipl.imports.service.ImportHandler;
import com.wmsdipl.imports.service.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

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
    private static final String IMPORT_FOLDER_KEY = "import_folder";

    private volatile Path importFolder;
    private volatile Path loadedFolder;
    private volatile Path errorFolder;
    private final ImportHandler importHandler;
    private final ImportConfigRepository configRepository;
    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    public ImportScheduler(@Value("${wms.import.folder:input}") String defaultImportFolder,
                           ImportHandler importHandler,
                           ImportConfigRepository configRepository) {
        this.importHandler = importHandler;
        this.configRepository = configRepository;
        
        // Load import folder from DB, or use default from application.yml
        String folderPath = loadImportFolderFromDb().orElse(defaultImportFolder);
        setImportFolderInternal(folderPath);
        log.info("Import folder initialized to: {}", this.importFolder.toAbsolutePath());
    }

    @Transactional
    public synchronized void updateImportFolder(String newFolder) {
        setImportFolderInternal(newFolder);
        processed.clear();
        
        // Save to database
        saveImportFolderToDb(newFolder);
        
        log.info("Import folder switched to {}", this.importFolder.toAbsolutePath());
    }

    public String getCurrentImportFolder() {
        return this.importFolder != null ? this.importFolder.toAbsolutePath().toString() : null;
    }

    private void setImportFolderInternal(String folder) {
        this.importFolder = Paths.get(folder);
        this.loadedFolder = this.importFolder.resolve("loaded");
        this.errorFolder = this.importFolder.resolve("error");
    }

    private java.util.Optional<String> loadImportFolderFromDb() {
        try {
            return configRepository.findByConfigKey(IMPORT_FOLDER_KEY)
                    .map(ImportConfig::getConfigValue);
        } catch (Exception e) {
            log.warn("Could not load import folder from database: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    private void saveImportFolderToDb(String folderPath) {
        try {
            ImportConfig config = configRepository.findByConfigKey(IMPORT_FOLDER_KEY)
                    .orElse(new ImportConfig());
            config.setConfigKey(IMPORT_FOLDER_KEY);
            config.setConfigValue(folderPath);
            configRepository.save(config);
            log.info("Import folder saved to database: {}", folderPath);
        } catch (Exception e) {
            log.error("Failed to save import folder to database: {}", e.getMessage(), e);
        }
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
