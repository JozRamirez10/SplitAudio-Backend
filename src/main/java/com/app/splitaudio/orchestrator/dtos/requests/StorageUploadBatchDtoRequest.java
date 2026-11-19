package com.app.splitaudio.orchestrator.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StorageUploadBatchDtoRequest(
    @JsonProperty("bucket_name") String bucketName,
    @JsonProperty("object_name") String objectName,
    @JsonProperty("local_file_paths") List<String> localFilePaths
) {}
