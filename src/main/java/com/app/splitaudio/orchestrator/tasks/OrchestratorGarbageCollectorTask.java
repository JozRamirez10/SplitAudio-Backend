package com.app.splitaudio.orchestrator.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
public class OrchestratorGarbageCollectorTask {

    @Value("${app.orchestrator.shared_dir}")
    private String shareTmpDir;

    private static final String PROCESS_NAME = "GARBAGE COLLECTOR";

    private static final long RETENTION_HOURS = 2L;

    @Scheduled(cron = "0 0 0/6 * * *")
    public void cleanupOrphanFiles() {
        
        log.info("*** [{}] Starting orphan file scan in: {}", PROCESS_NAME, shareTmpDir);

        Path shareDir = Path.of(shareTmpDir);
        if (!Files.exists(shareDir)) {
            return;
        }

        Instant thresholdTime = Instant.now().minus(RETENTION_HOURS, ChronoUnit.HOURS);
        int deletedItems = 0;

        try (Stream<Path> stream = Files.list(shareDir)) {
            List<Path> paths = stream.toList();

            for (Path path : paths) {

                if (path.getFileName().toString().startsWith(".")) {
                    continue;
                }

                try {
                    BasicFileAttributes attr = Files.readAttributes(path, 
                        BasicFileAttributes.class);

                    Instant lastModified = attr.lastModifiedTime().toInstant();

                    if (lastModified.isBefore(thresholdTime)) {
                        if (Files.isDirectory(path)) {
                            FileSystemUtils.deleteRecursively(path);
                            log.info("*** [{}] Temporary folder deleted: {}", 
                                PROCESS_NAME, path.getFileName());
                        } else {
                            Files.deleteIfExists(path);
                            log.info("*** [{}] Orphan file deleted: {}", PROCESS_NAME, 
                                path.getFileName());
                        }
                        deletedItems++;
                    }
                } catch (IOException e) {
                    log.warn("*** [{}] Could not analyze item: {}", PROCESS_NAME, 
                        path.getFileName());
                    
                }
            }

            if (deletedItems > 0) {
                log.info("*** [{}] Cleanup completed. {} item(s) deleted.", 
                    PROCESS_NAME, deletedItems);
            } else {
                log.info("*** [{}] System clean. No orphan items found.",
                    PROCESS_NAME);
            }
        } catch(IOException e) {
            log.error("*** [{}] Critical error reading temporary directory: {}", 
                PROCESS_NAME, shareTmpDir, e);
        }
    }
}
