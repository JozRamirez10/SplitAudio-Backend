package com.app.splitaudio.orchestrator.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SplitAudioDtoRequest<T extends Enum<T>>(
    @JsonProperty("input_path") String inputPath,
    @JsonProperty("desired_tracks") List<T> desiredTracks
) {}