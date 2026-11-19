package com.app.splitaudio.orchestrator.dtos.responses;

import com.app.splitaudio.songs.dtos.responses.SongDtoResponse;

public record OrchestratorDtoResponse(
    String bucketName,
    SongDtoResponse song
) {}