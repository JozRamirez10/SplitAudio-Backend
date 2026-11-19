package com.app.splitaudio.songs.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(
    name = "song",
    indexes = {
        @Index(name = "idx_song_user_email", columnList = "user_email")
    }
)
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Song {

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "song_name", nullable = false)
    private String songName;

    @Column(nullable = false)
    private Integer bpm;

    @Column(name = "tuning_hz", nullable = false)
    private Float tuningHz;

    @Column(name = "musical_key", nullable = false, length = 20)
    private String musicalKey;

    @Column(name = "minio_folder", nullable = false)
    private String minioFolder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SongTrack> tracks = new ArrayList<>();

    @PrePersist 
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void addTrack(SongTrack track) {
        tracks.add(track);
        track.setSong(this);
    }
}
