package com.app.splitaudio.songs.dtos.requests;

import java.util.List;

public record SongDtoRequest(
    String userEmail,
    String songName,
    Integer bpm,
    Float tuningHz,
    String musicalKey,
    String minioFolder,
    List<String> uploadedObjectKeys
) {}