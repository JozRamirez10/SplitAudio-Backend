package com.app.splitaudio.orchestrator.services;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.app.splitaudio.orchestrator.dtos.responses.ActiveJobsDtoResponse;
import com.app.splitaudio.orchestrator.dtos.responses.JobStatusResponse;
import com.app.splitaudio.orchestrator.dtos.responses.OrchestratorDtoResponse;
import com.app.splitaudio.orchestrator.enums.jobs.JobStatus;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
public class JobManagerService {

    private final ExecutorService jobQueue = Executors.newSingleThreadExecutor();

    private final Cache<String, JobStatusResponse> jobRegistry;

    public JobManagerService(
        @Value("${app.cache.time-expiration-minutes}") long timeExpirationMinutes,
        @Value("${app.cache.maximum-size}") long maximumSize
    ) {
        this.jobRegistry = Caffeine.newBuilder()
            .expireAfterWrite(timeExpirationMinutes, TimeUnit.MINUTES)
            .maximumSize(maximumSize)
            .build();
    }

    @PreDestroy
    public void shutdownQueue() {
        jobQueue.shutdown();
    }

    public Map<String, JobStatusResponse> getAllJobs() {
        return jobRegistry.asMap();
    }

    public ActiveJobsDtoResponse getActiveJobsSummary() {
        long activeJobs = jobRegistry.asMap().values().stream()
            .filter(job -> job.status() == JobStatus.PENDING || 
                    job.status() == JobStatus.PROCESSING)
            .count();

        return new ActiveJobsDtoResponse(activeJobs);
    }

    public JobStatusResponse getJobStatus(String jobId) {
        JobStatusResponse job = jobRegistry.getIfPresent(jobId);

        if (job == null) {
            return new JobStatusResponse(
                jobId, 
                JobStatus.NOT_FOUND, 
                0, 
                null, 
                null
            );
        }

        if (job.status() == JobStatus.PENDING) {
            int ahead = (int) jobRegistry.asMap().values().stream()
                .filter(j -> j.status() == JobStatus.PROCESSING || 
                    (j.status() == JobStatus.PENDING && j.createdAt().isBefore(job.createdAt()))
                )
                .count();
            return new JobStatusResponse(
                job.jobId(), job.status(), ahead, job.createdAt(), job.orchestratorDtoResponse()
            );
        }
        
        return job;
    }

    public void registerNewJob(String jobId) {
        jobRegistry.put(
            jobId, new JobStatusResponse(
                jobId,
                JobStatus.PENDING,
                0,
                Instant.now(),
                null
            )
        );
    }

    public void submitJob(String jobId, Callable<OrchestratorDtoResponse> pipelineTask) {
        jobQueue.submit(() -> {
            try {
                updateJobState(jobId, JobStatus.PROCESSING, null);

                OrchestratorDtoResponse response = pipelineTask.call();
                
                updateJobState(jobId, JobStatus.COMPLETED, response);

            } catch (Exception e) {
                log.error("Error processing job {}: {}", jobId, e.getMessage(), e);
                updateJobState(jobId, JobStatus.FAILED, null);
            }
        });
    }

    private void updateJobState(String jobId, JobStatus status, OrchestratorDtoResponse response) {
        JobStatusResponse currentJob = jobRegistry.getIfPresent(jobId);
        Instant createdAt = currentJob != null ? currentJob.createdAt() : Instant.now();

        jobRegistry.put(
            jobId, new JobStatusResponse(
                jobId,
                status,
                0,
                createdAt,
                response
            )
        );
    }
}
