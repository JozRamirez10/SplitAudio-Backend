package com.app.splitaudio.songs.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.app.splitaudio.songs.dtos.requests.SongDtoRequest;
import com.app.splitaudio.songs.dtos.responses.SongDtoResponse;

public interface SongService {

    Optional<SongDtoResponse> findById(UUID songId);

    List<SongDtoResponse> findByUserEmail(String userEmail);

    SongDtoResponse save(SongDtoRequest songDtoRequest);

    boolean deleteById(UUID songId);
}
