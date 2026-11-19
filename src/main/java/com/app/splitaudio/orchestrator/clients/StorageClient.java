package com.app.splitaudio.orchestrator.clients;

import com.app.splitaudio.orchestrator.dtos.requests.StorageDownloadDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.StorageUploadBatchDtoRequest;
import com.app.splitaudio.orchestrator.dtos.responses.StorageDownloadDtoResponse;
import com.app.splitaudio.orchestrator.dtos.responses.StorageUploadBatchDtoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "StorageApiClient", url = "${python.api.storage.url}")
public interface StorageClient {

    @PostMapping("/api/storage/upload")
    StorageUploadBatchDtoResponse uploadFilesBatch(@RequestBody StorageUploadBatchDtoRequest request);

    @PostMapping("/api/storage/download")
    StorageDownloadDtoResponse downloadFile(@RequestBody StorageDownloadDtoRequest request);
}
