package com.app.splitaudio.songs.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SongDtoResponse(
    UUID id,
    String userEmail,
    String songName,
    Integer bpm,
    Float tuningHz,
    String musicalKey,
    LocalDateTime createdAt,
    List<SongTrackDtoResponse> tracks
) {}