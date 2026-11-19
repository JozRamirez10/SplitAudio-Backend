package com.app.splitaudio.orchestrator.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageDownloadDtoResponse(
    @JsonProperty("file_path") String filePath
) {}