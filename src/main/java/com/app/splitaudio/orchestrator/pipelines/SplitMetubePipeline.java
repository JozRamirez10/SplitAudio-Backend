package com.app.splitaudio.orchestrator.pipelines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.app.splitaudio.orchestrator.clients.SplitMetubeClient;
import com.app.splitaudio.orchestrator.dtos.requests.SplitMetubeDtoRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@RequiredArgsConstructor
public class SplitMetubePipeline extends BasePipeline {

    private final SplitMetubeClient splitMetubeClient;

    private static final String SPLIT_SERVICE = "SPLIT-METUBE";

    private static final String QUALITY = "best";

    private static final String AUDIO_FORMAT = "wav";

    private static final long THREAD_SLEEP = 2000L;

    private static final long THREAD_SLEEP_SIZE = 1000L;

    private static final int MAX_DOWNLOAD_ATTEMPTS = 60;

    private static final int MAX_STABILITY_CHECKS = 60;

    private static final int REQUIRED_STABLE_CHECKS = 3;

    public CompletableFuture<Path> run(String url, Path metubeDownloadsDir, 
        AtomicBoolean killSwitch) {

        return executePipelineAsync(SPLIT_SERVICE, killSwitch, (pipelineName) -> {

            Set<String> existingFiles = getWavFiles(metubeDownloadsDir);

            log.info("*** [{}] 1/3 Sending download request to Metube for URL: {}", 
                pipelineName, url);

            ResponseEntity<String> response = step(killSwitch, () -> 
                splitMetubeClient.downloadAudio(
                    new SplitMetubeDtoRequest(url, QUALITY, AUDIO_FORMAT)
                )
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("MeTube API rejected the download. Http Status: "
                    + response.getStatusCode() + ", Response: " + response.getBody()
                );
            }

            log.info("*** [{}] INFO about the download: {}", pipelineName, 
                    response.getBody());
            
            log.info("*** [{}] 2/3 Waiting for download to complete... ", pipelineName);

            for (int i = 0; i < MAX_DOWNLOAD_ATTEMPTS; i++) {
                step(killSwitch, () -> true);
                sleepSafely(THREAD_SLEEP, "Metube");
                
                Set<String> currentFiles = getWavFiles(metubeDownloadsDir);
                currentFiles.removeAll(existingFiles);

                if (!currentFiles.isEmpty()) {
                    Path newFile = metubeDownloadsDir.resolve(currentFiles.iterator().next());
                    return waitForFileStability(newFile, pipelineName, killSwitch);
                }
            }
            throw new RuntimeException("Timeout waiting for Metube to download " + url);
        });
    }

    private Path waitForFileStability(Path newFile, String pipelineName, AtomicBoolean killSwitch) {
        
        log.info("*** [{}] File detected: {}. Waiting for FFmpeg conversion to finish...",
            pipelineName, newFile);

        long lastSize = -1;
        int stableCount = 0;

        for (int j = 0; j < MAX_STABILITY_CHECKS; j++) {
            step(killSwitch, () -> true);
            sleepSafely(THREAD_SLEEP_SIZE, "FFmpeg");

            try {
                long currentSize = Files.size(newFile);

                if (currentSize > 0 && currentSize == lastSize) {
                    stableCount++;
                    if (stableCount >= REQUIRED_STABLE_CHECKS) {
                        log.info("*** [{}] 3/3 Download completed! File detected: {}", 
                            pipelineName, newFile);
                        return newFile;
                    }
                } else {
                    stableCount = 0;
                    lastSize = currentSize;
                }
            } catch (IOException e) {
                log.warn("*** [{}] Error reading file size for {}, retrying...", 
                    pipelineName, newFile);
            }
        }
        
        throw new RuntimeException("Timeout waiting for FFmpeg to finish writing " + newFile);
    }

    private void sleepSafely(long millis, String context) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Pipeline interrupted while waiting for " + context);
        }
    }

    private Set<String> getWavFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(file -> !Files.isDirectory(file))
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.endsWith("." + AUDIO_FORMAT))
                .collect(Collectors.toSet());
        }
    }

}
