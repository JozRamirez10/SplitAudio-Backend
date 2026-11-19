package com.app.splitaudio.orchestrator.dtos.requests;

public record SplitMetubeDtoRequest(
    String url,
    String quality,
    String format
) {}
