package com.app.splitaudio.orchestrator.pipelines;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.app.splitaudio.orchestrator.clients.SplitMetronomeClient;
import com.app.splitaudio.orchestrator.dtos.requests.CleanupDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.SplitMetronomeDtoRequest;
import com.app.splitaudio.orchestrator.dtos.responses.SplitMetronomeDtoResponse;
import com.app.splitaudio.storage.services.MinioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service
@RequiredArgsConstructor  
public class SplitMetronomePipeline extends BasePipeline {

    private final MinioService minioService;
    private final SplitMetronomeClient metronomeClient;

    private static final String SPLIT_SERVICE = "SPLIT-METRONOME";

    public CompletableFuture<SplitMetronomeDtoResponse> run(Path localOriginalPath, 
        String minioBaseFolder, AtomicBoolean killSwitch) {

        return executePipelineAsync(SPLIT_SERVICE, killSwitch, (pipelineName) -> {
            Path localMetronomePath = null;
            
            try {
                log.info("*** [{}] 1/3 Generate metronome...", pipelineName);
    
                SplitMetronomeDtoRequest request = new SplitMetronomeDtoRequest(
                    localOriginalPath.toString()
                );
    
                SplitMetronomeDtoResponse response = step(killSwitch, () -> 
                    metronomeClient.processMetronome(request)
                );
    
                localMetronomePath = Path.of(response.outputPath());
                String minioMetronomePath = minioBaseFolder + localMetronomePath.getFileName()
                    .toString();
    
                log.info("*** [{}] 2/3 Upload metronome file {} to MinIO...", pipelineName, 
                    minioMetronomePath);
                
                final Path uploadPath = localMetronomePath;
                step(killSwitch, () -> minioService.uploadFile(uploadPath, minioMetronomePath));

                return new SplitMetronomeDtoResponse(
                    minioMetronomePath,
                    response.bpm(),
                    response.tuningHz(),
                    response.musicalKey()
                );

            } finally {

                if (localMetronomePath != null) {

                    final String pathToDelete = localMetronomePath.toString();
                    
                    log.info("*** [{}] 3/3 Cleaning up local metronome file: {}", 
                        pipelineName, pathToDelete);

                    executeCleanup(pipelineName, () -> 
                        metronomeClient.cleanup(new CleanupDtoRequest(pathToDelete))
                    );
                }
            }
        });
    }
}
