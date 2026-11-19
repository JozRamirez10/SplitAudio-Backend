package com.app.splitaudio.orchestrator.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageDownloadDtoRequest(
    @JsonProperty("bucket_name") String bucketName,
    @JsonProperty("object_name") String objectName,
    @JsonProperty("destination_folder") String destinationFolder
) {}
