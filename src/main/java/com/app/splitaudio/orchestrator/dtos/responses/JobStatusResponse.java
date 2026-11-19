package com.app.splitaudio.orchestrator.dtos.responses;

import java.time.Instant;

import com.app.splitaudio.orchestrator.enums.jobs.JobStatus;

public record JobStatusResponse(
    String jobId,
    JobStatus status,
    Integer jobsAhead,
    Instant createdAt,
    OrchestratorDtoResponse orchestratorDtoResponse
) {}
