package com.app.splitaudio.constants;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final List<String> ALLOWED_EXTENSIONS = 
        List.of(".mp3", ".wav", ".flac", ".m4a", ".ogg");

    public static final class App {

        public static final String APP_OCTET_STREAM = "application/octet-stream";
    }

    public static final class Errors {
        
        public static final String ERROR = "error";

    }

    public static final class Exceptions {

        public static final String RESOURCE_NOT_FOUND = "Resource not found";

    }
}