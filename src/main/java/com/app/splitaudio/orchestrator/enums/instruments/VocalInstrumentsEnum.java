package com.app.splitaudio.orchestrator.enums.instruments;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VocalInstrumentsEnum {

    VOCAL("Vocals"),
    CHORUS("Instruments");

    @JsonValue
    private final String value;
}
