package com.app.splitaudio.orchestrator.dtos.requests;

import com.app.splitaudio.orchestrator.enums.modes.DrumsModeEnum;
import com.app.splitaudio.orchestrator.enums.modes.GuitarModeEnum;
import com.app.splitaudio.orchestrator.enums.modes.VocalsModeEnum;
import com.app.splitaudio.orchestrator.enums.modes.WindsModeEnum;

public record OrchestratorDtoRequest(
    boolean includeBass,
    boolean includePiano,
    DrumsModeEnum drumsMode,
    GuitarModeEnum guitarMode,
    VocalsModeEnum vocalsMode,
    WindsModeEnum windsMode
) {}