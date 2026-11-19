package com.app.splitaudio.orchestrator.pipelines;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.app.splitaudio.orchestrator.clients.SplitAudioClient;
import com.app.splitaudio.orchestrator.clients.StorageClient;
import com.app.splitaudio.orchestrator.dtos.requests.CleanupDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.OrchestratorDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.SplitAudioDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.StorageDownloadDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.StorageUploadBatchDtoRequest;
import com.app.splitaudio.orchestrator.enums.instruments.TrackInstrumentsEnum;
import com.app.splitaudio.orchestrator.stemprocessors.StemProcessor;
import com.app.splitaudio.storage.services.MinioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@RequiredArgsConstructor 
public class SplitAudioPipeline extends BasePipeline {

    private final MinioService minioService;
    private final StorageClient storageClient;
    private final SplitAudioClient splitAudioClient;

    private final List<StemProcessor> stemProcessors;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${app.orchestrator.split_audio.tmp_dir}")
    private String splitAudioTmpDir;

    private static final String SPLIT_SERVICE = "SPLIT-AUDIO";

    private static final String NONE = "NONE";

    public CompletableFuture<List<String>> run(
        Path localOriginalPath, 
        String minioBaseFolder, 
        String originalFilename, 
        String uniqueId,
        OrchestratorDtoRequest tracks,
        AtomicBoolean killSwitch
    ) {
        String minioAudioPath = minioBaseFolder + originalFilename;

        return executePipelineAsync(SPLIT_SERVICE, killSwitch, (pipelineName) -> {
            
            Set<String> foldersToClean = ConcurrentHashMap.newKeySet();
            
            try {
                String splitLocalFile = prepareWorkerFile(
                    pipelineName, localOriginalPath, minioAudioPath, uniqueId, foldersToClean, 
                    killSwitch
                );

                List<String> tracksOutputPaths = executeBaseSeparation(
                    pipelineName, splitLocalFile, tracks, foldersToClean, killSwitch
                );

                List<String> finalPathsToUpload = processSubStem(
                    pipelineName, tracksOutputPaths, tracks, foldersToClean, killSwitch
                );
                
                return uploadFinalTracks(pipelineName, minioBaseFolder, finalPathsToUpload, 
                    killSwitch);

            } finally {
                cleanupWorkerFolders(pipelineName, foldersToClean);
            }
        });
    }

    private List<String> processSubStem(
        String pipelineName,
        List<String> tracksOutputPaths,
        OrchestratorDtoRequest tracks,
        Set<String> foldersToClean,
        AtomicBoolean killSwitch
    ) throws Exception {

        List<CompletableFuture<List<String>>> tasks = new ArrayList<>();
        List<String> finalPaths = new ArrayList<>(tracksOutputPaths);
        List<String> originalStemsToRemove = new ArrayList<>();

        List<StemProcessor> sortedProcessors = stemProcessors.stream()
            .sorted(Comparator.comparing(StemProcessor::isAsync).reversed())
            .toList();

        try {
            for (StemProcessor processor : sortedProcessors) {
                if (!processor.isApplicable(tracks)) {
                    continue;
                }
    
                String targetStemKeyword = processor.getTargetStemKeyword();
                Optional<String> targetStemOpt = findSpecificStem(tracksOutputPaths, 
                    targetStemKeyword);
                
                if (targetStemOpt.isEmpty()) {
                    log.warn("*** [{}] Applicable but stem {} not found", pipelineName, 
                        targetStemKeyword);
                    continue;
                }
    
                String targetStem = targetStemOpt.get();
                originalStemsToRemove.add(targetStem);
    
                if (processor.isAsync()) {
                    CompletableFuture<List<String>> asyncTask = CompletableFuture
                        .supplyAsync(() -> {
                            try {
                                return executeAndRegister(
                                    processor, targetStem, tracks, foldersToClean, killSwitch
                                );
                            } catch (Exception e) {
                                killSwitch.set(true);
                                throw new CompletionException(e);
                            }
                        });
                    tasks.add(asyncTask);
                } else {
                    List<String> syncResult = executeAndRegister(
                        processor, targetStem, tracks, foldersToClean, killSwitch
                    );
                    tasks.add(CompletableFuture.completedFuture(syncResult));
                }
            }
    
            for (CompletableFuture<List<String>> task : tasks) {
                finalPaths.addAll(task.join());
            }

        } catch (Exception e) {
            killSwitch.set(true);
            for (CompletableFuture<List<String>> task: tasks) {
                try {
                    task.join();
                } catch (Exception ignored) {}
            }
            throw e;
        }

        finalPaths.removeAll(originalStemsToRemove);
        return finalPaths;
    }

    private List<String> executeAndRegister(
        StemProcessor processor,
        String targetStem,
        OrchestratorDtoRequest tracks,
        Set<String> foldersToClean,
        AtomicBoolean killSwitch
    ) {
        List<String> newStems = step(killSwitch, () -> 
            processor.execute(targetStem, tracks, killSwitch)
        );
        registerFoldersToClean(foldersToClean, newStems);
        return newStems;
    }

    private String prepareWorkerFile(
        String pipelineName, Path localOriginalPath, String minioAudioPath, String uniqueId, 
        Set<String> foldersToClean, AtomicBoolean killSwitch
    ) throws Exception {

        log.info("*** [{}] 1/6 Upload file to MinIO: {}", pipelineName, minioAudioPath);
        String uploadedObjectName = step(killSwitch, () -> 
            minioService.uploadFile(localOriginalPath, minioAudioPath)
        );

        log.info("*** [{}] 2/6 Download file to split audio...", pipelineName);
        StorageDownloadDtoRequest downloadRequest = new StorageDownloadDtoRequest(
            bucketName, uploadedObjectName, Path.of(splitAudioTmpDir, uniqueId).toString()
        );

        String splitLocalFile = step(killSwitch, () -> 
            storageClient.downloadFile(downloadRequest).filePath()
        );

        registerFolderToClean(foldersToClean, splitLocalFile);
        return splitLocalFile;
    }

    private List<String> executeBaseSeparation(
        String pipelineName, String splitLocalFile, OrchestratorDtoRequest tracks, 
        Set<String> foldersToClean, AtomicBoolean killSwitch
    ) throws Exception {

        log.info("*** [{}] 3/6 Execute split track instruments", pipelineName);

        List<TrackInstrumentsEnum> tracksToProcess = mapToTrackInstruments(tracks);

        SplitAudioDtoRequest<TrackInstrumentsEnum> trackRequest = new SplitAudioDtoRequest<>(
            splitLocalFile, tracksToProcess
        );

        List<String> baseOutputPaths = step(killSwitch, () -> 
            splitAudioClient.processTracks(trackRequest).outputPaths()
        );

        registerFoldersToClean(foldersToClean, baseOutputPaths);
        return baseOutputPaths;
    }

    private List<String> uploadFinalTracks(
        String pipelineName, String minioBaseFolder, List<String> finalPathsToUpload, 
        AtomicBoolean killSwitch
    ) {
        
        log.info("*** [{}] 4/6 Uploading {} tracks to MinIO...", pipelineName, 
            finalPathsToUpload.size());
        
        StorageUploadBatchDtoRequest uploadRequest = new StorageUploadBatchDtoRequest(
            bucketName, minioBaseFolder, finalPathsToUpload
        );

        step(killSwitch, () -> storageClient.uploadFilesBatch(uploadRequest));

        return finalPathsToUpload.stream()
            .map(localPath -> minioBaseFolder + Path.of(localPath).getFileName().toString())
            .toList();
    }

    private void registerFolderToClean(Set<String> foldersToClean, String filePath) {
        if (filePath != null && !filePath.isBlank()) {
            Path parent = Path.of(filePath).getParent();
            if (parent != null) {
                foldersToClean.add(parent.toString());
            }
        }
    }

    private void registerFoldersToClean(Set<String> foldersToClean, List<String> filePaths) {
        if (filePaths != null && !filePaths.isEmpty()) {
            filePaths.forEach(p -> registerFolderToClean(foldersToClean, p));
        }
    }

    private void cleanupWorkerFolders(String pipelineName, Set<String> foldersToClean) {
        log.info("*** [{}] 5/6 & 6/6 Cleaning up ", pipelineName);
        for (String folder : foldersToClean) {
            executeCleanup(pipelineName, () -> {
                splitAudioClient.cleanup(new CleanupDtoRequest(folder));
            });
        }
    }

    private Optional<String> findSpecificStem(List<String> paths, String stem) {
        String prefix = "_" + stem.toLowerCase();
        String expectedSuffix = prefix + ".";
        return paths.stream()
            .filter(p -> {
                String filename = Path.of(p).getFileName().toString().toLowerCase();
                int lastDotIndex = filename.lastIndexOf(".");
                if (lastDotIndex == -1) {
                    return filename.endsWith(prefix);
                }
                String nameWithDot = filename.substring(0, lastDotIndex + 1);
                return nameWithDot.endsWith(expectedSuffix);
            })
            .findFirst();
    }

    private List<TrackInstrumentsEnum> mapToTrackInstruments(OrchestratorDtoRequest tracks) {
        return Stream.of(
            tracks.includeBass() ? TrackInstrumentsEnum.BASS : null,
            tracks.includePiano() ? TrackInstrumentsEnum.PIANO : null,
            shouldInclude(tracks.drumsMode()) ? TrackInstrumentsEnum.DRUMS : null,
            shouldInclude(tracks.guitarMode()) ? TrackInstrumentsEnum.GUITAR : null,
            shouldInclude(tracks.vocalsMode()) ? TrackInstrumentsEnum.VOCALS : null,
            shouldInclude(tracks.windsMode()) ? TrackInstrumentsEnum.OTHER : null
        )
        .filter(Objects::nonNull)
        .toList();
    }

    private boolean shouldInclude(Enum<?> mode) {
        return mode != null && !mode.name().equalsIgnoreCase(NONE);
    }
}