package com.app.splitaudio.orchestrator.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.app.splitaudio.orchestrator.dtos.requests.OrchestratorDtoRequest;
import com.app.splitaudio.orchestrator.dtos.responses.ActiveJobsDtoResponse;
import com.app.splitaudio.orchestrator.dtos.responses.JobStatusResponse;
import com.app.splitaudio.orchestrator.dtos.responses.JobSubmissionResponse;
import com.app.splitaudio.orchestrator.dtos.responses.OrchestratorDtoResponse;
import com.app.splitaudio.orchestrator.services.JobManagerService;
import com.app.splitaudio.orchestrator.services.OrchestratorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/orchestrator")
@RequiredArgsConstructor
public class OrchestratorController {

    private final OrchestratorService orchestratorService;
    private final JobManagerService jobManagerService;

    private static final String AUDIO_PROCESS = "Split-Audio";
    
    private static final String YOUTUBE_PROCESS = "Split-Youtube";

    private static final String FILE = "file";

    private static final String EMAIL = "email";
    
    private static final String TRACKS = "tracks";
    
    private static final String URL = "url";
    
    @GetMapping("/jobs")
    public ResponseEntity<Map<String, JobStatusResponse>> getAllJobs() {
        return ResponseEntity.ok(jobManagerService.getAllJobs());
    }

    @GetMapping("/jobs/active")
    public ResponseEntity<ActiveJobsDtoResponse> getActiveJobsSummary() {
        return ResponseEntity.ok(jobManagerService.getActiveJobsSummary());
    }

    @GetMapping("/jobs/status/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(
            jobManagerService.getJobStatus(jobId)
        );
    }

    @PostMapping("/split-audio")
    public ResponseEntity<OrchestratorDtoResponse> processAudio(
        @RequestParam(FILE) MultipartFile file,
        @RequestParam(EMAIL) String email,
        @RequestPart(TRACKS) OrchestratorDtoRequest tracks
    ) throws Exception {

        logRequestDetails(AUDIO_PROCESS, file.getOriginalFilename(), email, tracks);

        OrchestratorDtoResponse response = 
            orchestratorService.processFullAudio(file, email, tracks);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/split-audio/job")
    public ResponseEntity<JobSubmissionResponse> processAudioJob(
        @RequestParam(FILE) MultipartFile file,
        @RequestParam(EMAIL) String email,
        @RequestPart(TRACKS) OrchestratorDtoRequest tracks
    ) throws Exception {
        
        logRequestDetails(AUDIO_PROCESS, file.getOriginalFilename(), email, tracks);

        return ResponseEntity.ok(
            orchestratorService.processFullAudioJob(file, email, tracks)
        );
    }

    @PostMapping("/split-youtube")
    public ResponseEntity<OrchestratorDtoResponse> processYoutube(
        @RequestParam(URL) String url,
        @RequestParam(EMAIL) String email,
        @RequestPart(TRACKS) OrchestratorDtoRequest tracks
    ) throws Exception {
        
        logRequestDetails(YOUTUBE_PROCESS, url, email, tracks);

        OrchestratorDtoResponse response = orchestratorService.processYoutube(url, email, tracks);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/split-youtube/job")
    public ResponseEntity<JobSubmissionResponse> processYoutubeJob(
        @RequestParam(URL) String url,
        @RequestParam(EMAIL) String email,
        @RequestPart(TRACKS) OrchestratorDtoRequest tracks
    ) throws Exception {
        
        logRequestDetails(YOUTUBE_PROCESS, url, email, tracks);

        return ResponseEntity.ok(
            orchestratorService.processYoutubeJob(url, email, tracks)
        );   
    }

    private void logRequestDetails(String process, String source, String email, 
        OrchestratorDtoRequest tracks) {

        log.info("=======================================================================");
        log.info("*** [{}] Request received for {} of user {}", process, source, email);
        log.info("*** Tracks desired: {}", tracks);
    }
}