package com.app.splitaudio.orchestrator.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SplitAudioDtoResponse(
    @JsonProperty("input_path") String inputPath,
    @JsonProperty("output_paths") List<String> outputPaths
) {}