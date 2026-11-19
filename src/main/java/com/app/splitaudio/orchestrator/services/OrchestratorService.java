package com.app.splitaudio.orchestrator.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import com.app.splitaudio.orchestrator.dtos.internals.SplitFile;
import com.app.splitaudio.orchestrator.dtos.requests.OrchestratorDtoRequest;
import com.app.splitaudio.orchestrator.dtos.responses.JobSubmissionResponse;
import com.app.splitaudio.orchestrator.dtos.responses.OrchestratorDtoResponse;
import com.app.splitaudio.orchestrator.dtos.responses.SplitMetronomeDtoResponse;
import com.app.splitaudio.orchestrator.enums.jobs.JobStatus;
import com.app.splitaudio.orchestrator.pipelines.SplitAudioPipeline;
import com.app.splitaudio.orchestrator.pipelines.SplitMetronomePipeline;
import com.app.splitaudio.orchestrator.pipelines.SplitMetubePipeline;
import com.app.splitaudio.orchestrator.utils.AudioFileUtils;
import com.app.splitaudio.songs.dtos.requests.SongDtoRequest;
import com.app.splitaudio.songs.dtos.responses.SongDtoResponse;
import com.app.splitaudio.songs.services.SongService;
import com.app.splitaudio.storage.services.MinioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final MinioService minioService;
    private final SongService songService;
    private final JobManagerService jobManagerService;

    private final SplitAudioPipeline splitAudioPipeline;
    private final SplitMetronomePipeline splitMetronomePipeline;
    private final SplitMetubePipeline splitMetubePipeline;
    
    @Value("${app.orchestrator.shared_dir}")
    private String shareTmpDir;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public OrchestratorDtoResponse processFullAudio(MultipartFile file, String email, 
        OrchestratorDtoRequest tracks) throws Exception {
        return executePipelines(prepareLocalFile(file), email, tracks);
    }

    public OrchestratorDtoResponse processYoutube(String url, String email, 
        OrchestratorDtoRequest tracks) throws Exception {
        return executePipelines(prepareYoutubeFile(url), email, tracks);
    }

    public JobSubmissionResponse processFullAudioJob(MultipartFile file, String email, 
        OrchestratorDtoRequest tracks) throws Exception {
        return submitAsyncJob(prepareLocalFile(file), email, tracks);
    }

    public JobSubmissionResponse processYoutubeJob(String url, String email, 
        OrchestratorDtoRequest tracks) throws Exception {
        return submitAsyncJob(prepareYoutubeFile(url), email, tracks);
    }

    private SplitFile prepareLocalFile(MultipartFile file) throws IOException {

        String uniqueId = UUID.randomUUID().toString();
        String rawFilename = Objects.requireNonNull(file.getOriginalFilename(), 
            "Uploaded file must have a valid filename");

        AudioFileUtils.validateFileExtension(rawFilename);
        Path baseDir = AudioFileUtils.createDirIntoBaseDir(shareTmpDir, uniqueId);
        Path localPath = AudioFileUtils.resolveNormalizePath(baseDir, rawFilename);

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, localPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return new SplitFile(uniqueId, localPath);
    }

    private SplitFile prepareYoutubeFile(String url) throws Exception {

        String uniqueId = UUID.randomUUID().toString();
        Path baseDir = AudioFileUtils.createDirIntoBaseDir(shareTmpDir, uniqueId);

        try {
            Path downloadedFile = splitMetubePipeline.run(url, Path.of(shareTmpDir), 
                new AtomicBoolean(false)).join();

            Path localPath = AudioFileUtils.resolveNormalizePath(baseDir, 
                downloadedFile.getFileName().toString());

            Files.copy(downloadedFile, localPath, StandardCopyOption.REPLACE_EXISTING);

            return new SplitFile(uniqueId, localPath);

        } catch (Exception e) {
            log.error("*** Youtube processing failed for url: {}. Aborting.", url, e);
            FileSystemUtils.deleteRecursively(baseDir);
            throw new Exception("Youtube processing failed", e);
        }
    }

    private JobSubmissionResponse submitAsyncJob(SplitFile splitFile, String email, 
        OrchestratorDtoRequest tracks) throws Exception {
        
        String jobId = splitFile.id();
        
        jobManagerService.registerNewJob(jobId);
        jobManagerService.submitJob(jobId, () -> executePipelines(splitFile, email, tracks));
        
        return new JobSubmissionResponse(jobId, JobStatus.PENDING);
    }

    private OrchestratorDtoResponse executePipelines(
        SplitFile splitFile, 
        String email, 
        OrchestratorDtoRequest tracks
    ) throws Exception {

        String originalFilename = splitFile.localFilePath().getFileName().toString();
        String songName = AudioFileUtils.extractSongName(originalFilename);
        String minioBaseFolder = email + "/" + songName + "/";

        AtomicBoolean killSwitch = new AtomicBoolean(false);

        try {

            log.info("*** Starting pipelines for song: {}", songName);

            CompletableFuture<List<String>> splitAudioPipelineFuture = splitAudioPipeline.run(
                splitFile.localFilePath(),
                minioBaseFolder,
                originalFilename,
                splitFile.id(),
                tracks,
                killSwitch
            );

            CompletableFuture<SplitMetronomeDtoResponse> splitMetronomePipelineFuture = 
                splitMetronomePipeline.run(
                
                splitFile.localFilePath(), 
                
                minioBaseFolder,
                killSwitch
            );

            CompletableFuture.allOf(splitAudioPipelineFuture, splitMetronomePipelineFuture).join();
            SplitMetronomeDtoResponse metronomeResult = splitMetronomePipelineFuture.join();

            List<String> allTrackPaths = Stream.concat(
                splitAudioPipelineFuture.join().stream(), 
                Stream.of(
                    metronomeResult.outputPath(), 
                    minioBaseFolder + originalFilename
                )
            ).toList();

            SongDtoResponse songSaved = songService.save(
                new SongDtoRequest(
                    email,
                    songName,
                    metronomeResult.bpm(),
                    metronomeResult.tuningHz(),
                    metronomeResult.musicalKey(),
                    minioBaseFolder,
                    allTrackPaths
                )
            );

            log.info("*** All pipelines finished successfully for: {}", songName);

            return new OrchestratorDtoResponse(
                bucketName,
                songSaved
            );

        } catch (Exception e) {
            log.error("*** Pipeline failed for song {}. Reason: {}. Aborting process.", 
                songName, e.getMessage());
            minioService.deleteFolder(minioBaseFolder);
            throw new Exception("Pipeline processing failed ", e);
        } finally {
            log.info("*** Cleaning up original uploaded file: {}", 
                splitFile.localFilePath());
            FileSystemUtils.deleteRecursively(splitFile.localFilePath().getParent());
        }
    }    
}