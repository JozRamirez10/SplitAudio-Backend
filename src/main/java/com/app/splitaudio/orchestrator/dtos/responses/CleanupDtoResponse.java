package com.app.splitaudio.orchestrator.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CleanupDtoResponse(
    @JsonProperty("path_deleted") String pathDeleted
) {}