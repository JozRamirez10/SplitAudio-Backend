package com.app.splitaudio.songs.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.splitaudio.exceptions.ResourceNotFoundException;
import com.app.splitaudio.songs.constants.Constants;
import com.app.splitaudio.songs.dtos.requests.SongDtoRequest;
import com.app.splitaudio.songs.dtos.responses.SongDtoResponse;
import com.app.splitaudio.songs.services.SongService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/songs")
@RequiredArgsConstructor 
public class SongController {

    private final SongService songService;

    @GetMapping("/{id}")
    public ResponseEntity<SongDtoResponse> getSongById(@PathVariable UUID id) {

        SongDtoResponse songDtoResponse = songService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(formatSongNotFoundMessage(id)));

        return ResponseEntity.ok(songDtoResponse);
    }

    @GetMapping
    public ResponseEntity<List<SongDtoResponse>> getSongsByUserEmail(
        @RequestParam String userEmail) {

        return ResponseEntity.ok(songService.findByUserEmail(userEmail));
    }
    
    @PostMapping
    public ResponseEntity<SongDtoResponse> saveSong(@RequestBody SongDtoRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(songService.save(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable UUID id) {
        
        if (!songService.deleteById(id)) {
            throw new ResourceNotFoundException(formatSongNotFoundMessage(id));
        }

        return ResponseEntity.noContent().build();
    }

    private static String formatSongNotFoundMessage(UUID id) {
        return String.format(Constants.Errors.SONG_NOT_FOUND, id);
    }
}
