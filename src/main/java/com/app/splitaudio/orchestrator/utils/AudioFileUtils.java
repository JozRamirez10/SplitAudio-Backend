package com.app.splitaudio.orchestrator.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;

import com.app.splitaudio.constants.Constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AudioFileUtils {

    public static String extractSongName(String filename) {
        return filename.contains(".") 
            ? filename.substring(0, filename.lastIndexOf(".")) 
            : filename;
    }

    public static Path resolveNormalizePath(Path baseDir, String filename) {
        String cleanName = Path.of(filename).getFileName().toString();
        String normalized = Normalizer.normalize(cleanName, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") 
            .replaceAll("\\s+", "_");
        return baseDir.resolve(normalized);
    }

    public static Path createDirIntoBaseDir(String baseDir, String dirToCreate) throws IOException {
        return Files.createDirectories(Path.of(baseDir, dirToCreate));
    }

    public static void validateFileExtension(String filename) {
        String lowerCaseName = filename.toLowerCase();
        if (Constants.ALLOWED_EXTENSIONS.stream().noneMatch(lowerCaseName::endsWith)) {
            throw new IllegalArgumentException("Unsupported file format. Allowed extensions: " + 
                Constants.ALLOWED_EXTENSIONS);
        }
    }
}
