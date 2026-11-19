package com.app.splitaudio.orchestrator.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SplitMetronomeDtoResponse( 
    @JsonProperty("output_path") String outputPath,
    int bpm,
    @JsonProperty("tuning_hz") float tuningHz,
    @JsonProperty("musical_key") String musicalKey
) {}