package com.app.splitaudio.songs.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.splitaudio.songs.entities.Song;

public interface SongRepository extends JpaRepository<Song, UUID> {

    @Override 
    @EntityGraph(attributePaths = {"tracks"})
    Optional<Song> findById(UUID id);

    @EntityGraph(attributePaths = {"tracks"})
    List<Song> findByUserEmailOrderByCreatedAtDesc(String userEmail);

}
