package com.app.splitaudio.orchestrator.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.app.splitaudio.orchestrator.dtos.requests.SplitMetubeDtoRequest;

@FeignClient(name = "SplitMetubeApiClient", url = "${metube.url}")
public interface SplitMetubeClient {

    @PostMapping("/add")
    ResponseEntity<String> downloadAudio(@RequestBody SplitMetubeDtoRequest request);

}
