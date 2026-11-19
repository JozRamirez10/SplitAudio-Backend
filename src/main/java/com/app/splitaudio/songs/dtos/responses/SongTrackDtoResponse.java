package com.app.splitaudio.songs.dtos.responses;

import java.util.UUID;

public record SongTrackDtoResponse(
    UUID id,
    String trackType,
    String presignedUrl
) {}