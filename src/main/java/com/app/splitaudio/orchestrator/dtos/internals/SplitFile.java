package com.app.splitaudio.orchestrator.dtos.internals;

import java.nio.file.Path;

public record SplitFile(
    String id,
    Path localFilePath
) {}