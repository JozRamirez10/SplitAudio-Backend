package com.app.splitaudio.orchestrator.dtos.responses;

import com.app.splitaudio.orchestrator.enums.jobs.JobStatus;

public record JobSubmissionResponse(
    String jobId,
    JobStatus status
) {}