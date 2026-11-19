package com.app.splitaudio.orchestrator.enums.instruments;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GuitarRoleInstrumentsEnum {

    RHYTHM("rythm_guitar"),
    LEAD("lead_guitar");

    @JsonValue
    private final String value;
}
