package com.app.splitaudio.orchestrator.clients;

import com.app.splitaudio.orchestrator.dtos.requests.CleanupDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.SplitMetronomeDtoRequest;
import com.app.splitaudio.orchestrator.dtos.responses.CleanupDtoResponse;
import com.app.splitaudio.orchestrator.dtos.responses.SplitMetronomeDtoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "SplitMetronomeApiClient", url = "${python.api.splitmetronome.url}")
public interface SplitMetronomeClient {

    @PostMapping("/api/process-metronome")
    SplitMetronomeDtoResponse processMetronome(@RequestBody SplitMetronomeDtoRequest request);

    @DeleteMapping("/api/cleanup")
    CleanupDtoResponse cleanup(@RequestBody CleanupDtoRequest request);
}
