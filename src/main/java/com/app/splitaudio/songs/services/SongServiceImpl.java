package com.app.splitaudio.songs.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.splitaudio.songs.dtos.requests.SongDtoRequest;
import com.app.splitaudio.songs.dtos.responses.SongDtoResponse;
import com.app.splitaudio.songs.dtos.responses.SongTrackDtoResponse;
import com.app.splitaudio.songs.entities.Song;
import com.app.splitaudio.songs.entities.SongTrack;
import com.app.splitaudio.songs.repositories.SongRepository;
import com.app.splitaudio.storage.services.MinioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@RequiredArgsConstructor 
public class SongServiceImpl implements SongService {
    
    private final SongRepository songRepository;
    private final MinioService minioService;

    private static final String ORIGINAL_TRACK = "original";

    @Override
    @Transactional(readOnly = true)
    public Optional<SongDtoResponse> findById(UUID songId) {
        return songRepository.findById(songId)
            .map(this::mapToDtoWithPresignedUrls);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongDtoResponse> findByUserEmail(String userEmail) {
        return songRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
            .stream()
            .map(this::mapToDtoWithPresignedUrls)
            .toList();
    }
    
    @Override
    @Transactional 
    public SongDtoResponse save(SongDtoRequest songDtoRequest) {

        String songName = songDtoRequest.songName();
        String userEmail = songDtoRequest.userEmail();

        log.info("*** Saving song {} for user {} in database...", songName, userEmail);

        Song song = Song.builder()
            .userEmail(userEmail)
            .songName(songName)
            .bpm(songDtoRequest.bpm())
            .tuningHz(songDtoRequest.tuningHz())
            .musicalKey(songDtoRequest.musicalKey())
            .minioFolder(songDtoRequest.minioFolder())
            .build();

        for (String objectKey : songDtoRequest.uploadedObjectKeys()) {
            String trackType = extractType(objectKey, songName);
            SongTrack track = SongTrack.builder()
                .trackType(trackType)
                .minioObjectKey(objectKey)
                .build();
            song.addTrack(track);
        }

        Song songSaved = songRepository.save(song);

        log.info("*** Song saved successfully with ID: {}", songSaved.getId());

        return mapToDtoWithPresignedUrls(songSaved);
    }

    @Override
    @Transactional
    public boolean deleteById(UUID songId) {

        Optional<Song> songOpt = songRepository.findById(songId);

        if (songOpt.isPresent()) {

            Song song = songOpt.get();
            String minioFolder = song.getMinioFolder();

            log.info("*** Deleting MinIO folder {} and db records for song ID {}", 
                minioFolder, songId);

            minioService.deleteFolder(minioFolder);
            songRepository.delete(song);

            return true;
        }

        return false;
    }

    private String extractType(String objectKey, String songName) {
        String filename = objectKey.substring(objectKey.lastIndexOf("/") + 1);
        String nameWithoutExt = filename.contains(".")
            ? filename.substring(0, filename.lastIndexOf("."))
            : filename;
        
        if (nameWithoutExt.equals(songName)) {
            return ORIGINAL_TRACK;
        }
        
        String prefix = songName + "_";
        if (nameWithoutExt.startsWith(prefix)) {
            return nameWithoutExt.substring(prefix.length());
        }
        return nameWithoutExt;
    }

    private SongDtoResponse mapToDtoWithPresignedUrls(Song song) {
        List<SongTrackDtoResponse> trackDtos = song.getTracks().stream()
            .map(track -> new SongTrackDtoResponse(
                track.getId(),
                track.getTrackType(),
                minioService.getPresignedUrl(track.getMinioObjectKey())
            ))
            .toList();
        
        return new SongDtoResponse(
            song.getId(),
            song.getUserEmail(),
            song.getSongName(),
            song.getBpm(),
            song.getTuningHz(),
            song.getMusicalKey(),
            song.getCreatedAt(),
            trackDtos
        );
    }

}