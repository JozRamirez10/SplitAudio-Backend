package com.app.splitaudio.orchestrator.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StorageUploadBatchDtoResponse(
    @JsonProperty("file_paths") List<String> filePaths
) {}