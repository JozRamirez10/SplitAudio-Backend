package com.app.splitaudio.orchestrator.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SplitMetronomeDtoRequest(
    @JsonProperty("input_path") String inputPath
) {}