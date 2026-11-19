package com.app.splitaudio.orchestrator.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CleanupDtoRequest(
    @JsonProperty("target_path") String targetPath
) {}
